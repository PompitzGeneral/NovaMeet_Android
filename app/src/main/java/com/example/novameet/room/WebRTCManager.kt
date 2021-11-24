package com.example.novameet.room

import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.webrtcmultipleandroidsample.WebRTC.PeerConnectionProperties
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.novameet.R
import com.example.novameet.model.SurfaceViewRVItem
import com.example.novameet.model.User
import com.example.novameet.room.WebRTC.*
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.webrtc.*
import org.webrtc.audio.AudioDeviceModule
import org.webrtc.audio.JavaAudioDeviceModule
import java.net.URISyntaxException
import java.util.ArrayList

class WebRTCManager(
    var applicationContext: Context,
    var videoSinkRecyclerView: RecyclerView,
    var peerConnectionParameters: PeerConnectionParameters?,
    var roomConnectionParameters: RoomConnectionParameters?,
    var rootEglBase: EglBase?)
    : PeerConnectionEvents
{
    private val TAG : String = "WebRTCManager"
    // For WebRTC
    private val rvItemLocalSocketID: String? = "local"
    private var signalingSocket: Socket? = IO.socket(roomConnectionParameters?.signalingServerUrl)
    private var socketIDToPeerConnectionPairs = mutableMapOf<String, PeerConnectionClient?>()
    private var socketIDToUserPairs = mutableMapOf<String, User?>()

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var localVideoTrack: VideoTrack? = null

    private var signalingParameters: SignalingParameters? = null
    private var audioManager: AppRTCAudioManager? = null

    private var logToast: Toast? = null
    private var activityRunning = false

    private var isError = false
    private var callStartedTimeMs: Long = 0
    private var screencaptureEnabled = false
    private var mediaProjectionPermissionResultData: Intent? = null
    private var mediaProjectionPermissionResultCode = 0
    private var useCamara2 = true
    private var isCaptureToTexture = true

    private var isVideoEnabled = true
    private var isAudioEnabled = true

    private val CAPTURE_PERMISSION_REQUEST_CODE = 1
    private val STAT_CALLBACK_PERIOD = 1000

    // For invoke code in UI Thread
    private val handler = Handler(Looper.getMainLooper());

    fun start() {
        initPeerConnectionFactory()
        startStreamingLocalVideo()
        startCall()
    }

    fun stop() {
        Thread.setDefaultUncaughtExceptionHandler(null)

        signalingSocket?.disconnect()
        signalingSocket = null

        (videoSinkRecyclerView?.adapter as SurfaceViewAdapter)?.clearItems()

        socketIDToPeerConnectionPairs?.let {
            for (sidToPCPair in it) {
                sidToPCPair?.value?.stopVideoSource()
                sidToPCPair?.value?.close();
            }
            it.clear()
        }

        audioManager?.stop()
        audioManager = null

        rootEglBase?.release()

        runOnUiThread {
            logToast?.cancel()
        }
    }

    fun setUseCamara2(useCamara2: Boolean) {
        this.useCamara2 = useCamara2
    }

    fun setIsCaptureToTexture(isCaptureToTexture: Boolean) {
        this.isCaptureToTexture = isCaptureToTexture
    }

    private fun initPeerConnectionFactory() {
        var fieldTrials: String? = getFieldTrials()

        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(applicationContext)
                .setFieldTrials(fieldTrials)
                .setEnableInternalTracer(true)
                .createInitializationOptions()
        )

        val audioDeviceModule: AudioDeviceModule? = createJavaAudioDevice()
        val enableH264HighProfile = PeerConnectionProperties.VIDEO_CODEC_H264_HIGH == peerConnectionParameters!!.videoCodec
        val encoderFactory: VideoEncoderFactory
        val decoderFactory: VideoDecoderFactory

        if (peerConnectionParameters?.videoCodecHwAcceleration ?: false) {
            encoderFactory = DefaultVideoEncoderFactory(
                rootEglBase?.getEglBaseContext(),
                true /* enableIntelVp8Encoder */,
                enableH264HighProfile
            )
            decoderFactory = DefaultVideoDecoderFactory(rootEglBase?.getEglBaseContext())
        } else {
            encoderFactory = SoftwareVideoEncoderFactory()
            decoderFactory = SoftwareVideoDecoderFactory()
        }

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setOptions(PeerConnectionFactory.Options())
            .setAudioDeviceModule(audioDeviceModule)
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory()

        Log.d(TAG, "Peer connection factory created.")
        audioDeviceModule?.release()
    }

    private fun getFieldTrials(): String? {
        var fieldTrials: String? = ""
        if (peerConnectionParameters?.videoFlexfecEnabled ?: false) {
            fieldTrials += PeerConnectionProperties.VIDEO_FLEXFEC_FIELDTRIAL
            Log.d(TAG, "Enable FlexFEC field trial.")
        }
        fieldTrials += PeerConnectionProperties.VIDEO_VP8_INTEL_HW_ENCODER_FIELDTRIAL
        if (peerConnectionParameters?.disableWebRtcAGCAndHPF ?: false) {
            fieldTrials += PeerConnectionProperties.DISABLE_WEBRTC_AGC_FIELDTRIAL
            Log.d(TAG, "Disable WebRTC AGC field trial.")
        }
        return fieldTrials
    }

    private fun createJavaAudioDevice(): AudioDeviceModule? {
        // Enable/disable OpenSL ES playback.
        if (!peerConnectionParameters!!.useOpenSLES) {
            Log.w(TAG, "External OpenSLES ADM not implemented yet.")
            // TODO(magjed): Add support for external OpenSLES ADM.
        }

        // Set audio record error callbacks.
        val audioRecordErrorCallback: JavaAudioDeviceModule.AudioRecordErrorCallback = object :
            JavaAudioDeviceModule.AudioRecordErrorCallback {
            override fun onWebRtcAudioRecordInitError(errorMessage: String) {
                Log.e(TAG,"onWebRtcAudioRecordInitError: $errorMessage")
            }

            override fun onWebRtcAudioRecordStartError(
                errorCode: JavaAudioDeviceModule.AudioRecordStartErrorCode, errorMessage: String
            ) {
                Log.e(TAG,"onWebRtcAudioRecordStartError: $errorCode. $errorMessage")
            }

            override fun onWebRtcAudioRecordError(errorMessage: String) {
                Log.e(TAG, "onWebRtcAudioRecordError: $errorMessage")
            }
        }
        val audioTrackErrorCallback: JavaAudioDeviceModule.AudioTrackErrorCallback = object :
            JavaAudioDeviceModule.AudioTrackErrorCallback {
            override fun onWebRtcAudioTrackInitError(errorMessage: String) {
                Log.e(TAG,"onWebRtcAudioTrackInitError: $errorMessage")
            }

            override fun onWebRtcAudioTrackStartError(
                errorCode: JavaAudioDeviceModule.AudioTrackStartErrorCode, errorMessage: String
            ) {
                Log.e(TAG,"onWebRtcAudioTrackStartError: $errorCode. $errorMessage")
            }

            override fun onWebRtcAudioTrackError(errorMessage: String) {
                Log.e(TAG, "onWebRtcAudioTrackError: $errorMessage")
            }
        }

        // Set audio record state callbacks.
        val audioRecordStateCallback: JavaAudioDeviceModule.AudioRecordStateCallback = object :
            JavaAudioDeviceModule.AudioRecordStateCallback {
            override fun onWebRtcAudioRecordStart() {
                Log.i(TAG, "Audio recording starts")
            }

            override fun onWebRtcAudioRecordStop() {
                Log.i(TAG, "Audio recording stops")
            }
        }

        // Set audio track state callbacks.
        val audioTrackStateCallback: JavaAudioDeviceModule.AudioTrackStateCallback = object :
            JavaAudioDeviceModule.AudioTrackStateCallback {
            override fun onWebRtcAudioTrackStart() {
                Log.i(TAG, "Audio playout starts")
            }

            override fun onWebRtcAudioTrackStop() {
                Log.i(TAG, "Audio playout stops")
            }
        }
        return JavaAudioDeviceModule.builder(applicationContext)
            .setUseHardwareAcousticEchoCanceler(!peerConnectionParameters!!.disableBuiltInAEC)
            .setUseHardwareNoiseSuppressor(!peerConnectionParameters!!.disableBuiltInNS)
            .setAudioRecordErrorCallback(audioRecordErrorCallback)
            .setAudioTrackErrorCallback(audioTrackErrorCallback)
            .setAudioRecordStateCallback(audioRecordStateCallback)
            .setAudioTrackStateCallback(audioTrackStateCallback)
            .createAudioDeviceModule()
    }

    private fun startStreamingLocalVideo() {
        var localVideoCapturer = createVideoCapturer()
        localVideoTrack = createVideoTrack(localVideoCapturer)

        var newSurfaceViewRVItem = SurfaceViewRVItem(
            rvItemLocalSocketID,
            roomConnectionParameters?.userInfo?.userID,
            roomConnectionParameters?.userInfo?.userDisplayName,
            roomConnectionParameters?.userInfo?.userImageUrl,
            isVideoEnabled,
            isAudioEnabled,
            localVideoTrack)

        (videoSinkRecyclerView?.adapter as SurfaceViewAdapter).addItem(newSurfaceViewRVItem)
    }

    private fun createVideoCapturer(): VideoCapturer? {
        val videoCapturer: VideoCapturer? =
        if (screencaptureEnabled) {
            return createScreenCapturer()
        } else if (useCamera2()) {
            if (!isCaptureToTexture) {
                reportError(applicationContext.getString(R.string.camera2_texture_only_error))
                return null
            }
            Logging.d(TAG, "Creating capturer using camera2 API.")
            createCameraCapturer(Camera2Enumerator(applicationContext))
        } else {
            Logging.d(TAG, "Creating capturer using camera1 API.")
            createCameraCapturer(Camera1Enumerator(isCaptureToTexture))
        }

        if (videoCapturer == null) {
            reportError("Failed to open camera")
        }
        return videoCapturer
    }

    private fun createCameraCapturer(enumerator: CameraEnumerator): VideoCapturer? {
        val deviceNames = enumerator.deviceNames
        // First, try to find front facing camera
        Logging.d(TAG, "Looking for front facing cameras.")
        for (deviceName in deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                Logging.d(TAG, "Creating front facing camera capturer.")
                val videoCapturer: VideoCapturer? = enumerator.createCapturer(deviceName, null)
                if (videoCapturer != null) {
                    return videoCapturer
                }
            }
        }
        // Front facing camera not found, try something else
        Logging.d(TAG, "Looking for other cameras.")
        for (deviceName in deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                Logging.d(TAG, "Creating other camera capturer.")
                val videoCapturer: VideoCapturer? = enumerator.createCapturer(deviceName, null)
                if (videoCapturer != null) {
                    return videoCapturer
                }
            }
        }
        return null
    }

    private fun createVideoTrack(capturer: VideoCapturer?): VideoTrack? {
        var surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", rootEglBase?.getEglBaseContext())
        var videoSource = peerConnectionFactory?.createVideoSource(capturer!!.isScreencast)
        capturer?.initialize(surfaceTextureHelper, applicationContext, videoSource?.getCapturerObserver())
        capturer?.startCapture(240, 240, 60)
        var videoTrack = peerConnectionFactory?.createVideoTrack(PeerConnectionProperties.VIDEO_TRACK_ID, videoSource)
        videoTrack?.setEnabled(true)
        return videoTrack
    }

    private fun useCamera2(): Boolean {
        return Camera2Enumerator.isSupported(applicationContext) && useCamara2
    }

    @TargetApi(21)
    private fun createScreenCapturer(): VideoCapturer? {
        if (mediaProjectionPermissionResultCode != AppCompatActivity.RESULT_OK) {
            reportError("User didn't give permission to capture the screen.")
            return null
        }
        return ScreenCapturerAndroid(
            mediaProjectionPermissionResultData, object : MediaProjection.Callback() {
                override fun onStop() {
                    reportError("User revoked permission to capture the screen.")
                }
            })
    }

    fun onCameraSwitch() {
        // Todo.
        // peerConnectionClient?.switchCamera()
    }

    fun onVideoScalingSwitch(scalingType: RendererCommon.ScalingType?) {
        //binding.fullscreenVideoView.setScalingType(scalingType)
    }

    fun onCaptureFormatChange(width: Int, height: Int, framerate: Int) {
        // Todo.
        // peerConnectionClient?.changeCaptureFormat(width, height, framerate)
    }

    fun setVideoEnabled(isEnabled: Boolean) {
        // 비디오 상태 변경
        isVideoEnabled = isEnabled
        localVideoTrack?.setEnabled(isEnabled)

        // Todo. 211119 DGLEE - 꺼진 video 화면에 아바타 표시
//        var videoSinkRVAdapter = videoSinkRecyclerView.adapter as SurfaceViewAdapter
//        videoSinkRVAdapter.updateItemIsEnabeld(rvItemLocalSocketID, "video", isEnabled)
        // 화상채팅방 참여자들에게 상태변경 메시지 전송
        sendMediaEnabledChanged(isEnabled, "video");
    }

    fun setMicEnabled(isEnabled: Boolean) {
        // 마이크 상태 변경
        isAudioEnabled = isEnabled
        socketIDToPeerConnectionPairs?.let {
            for (sidToPCPair in socketIDToPeerConnectionPairs) {
                sidToPCPair?.value?.setAudioEnabled(isEnabled)
            }
        }
        // 마이크 상태표시 변경
        var videoSinkRVAdapter = videoSinkRecyclerView.adapter as SurfaceViewAdapter
        videoSinkRVAdapter.updateItemIsEnabeld(rvItemLocalSocketID, "audio", isEnabled)
        // 화상채팅방 참여자들에게 상태변경 메시지 전송
        sendMediaEnabledChanged(isEnabled, "audio");
    }

    // Should be called from UI thread
    private fun callConnected(remoteUserSocketID: String?) {
        val delta = System.currentTimeMillis() - callStartedTimeMs
        Log.i(TAG, "Call connected: delay=" + delta + "ms")

        var peerConnectionClient = this.socketIDToPeerConnectionPairs?.get(remoteUserSocketID)

        if (peerConnectionClient == null || isError) {
            Log.w(TAG, "Call is connected in closed or error state")
            return
        }
        // Enable statistics callback.
        peerConnectionClient?.enableStatsEvents(true, STAT_CALLBACK_PERIOD)
    }

    // This method is called when the audio manager reports audio device change,
    // e.g. from wired headset to speakerphone.
    private fun onAudioManagerDevicesChanged(
        device: AppRTCAudioManager.AudioDevice?, availableDevices: Set<AppRTCAudioManager.AudioDevice?>?
    ) {
        Log.d(TAG, "onAudioManagerDevicesChanged: " + availableDevices + ", "
                + "selected: " + device
        )
        // TODO(henrika): add callback handler.
    }

    private fun receivedConnectedEvent() {
        Log.d(TAG,"receivedConnectedEvent")
        val message = JSONObject()
        message.put("room", roomConnectionParameters?.roomId)
        message.put("userID", roomConnectionParameters?.userInfo?.userID)
        message.put("userDisplayName", roomConnectionParameters?.userInfo?.userDisplayName)
        message.put("userImageUrl", roomConnectionParameters?.userInfo?.userImageUrl)
        message.put("isVideoEnabled", isVideoEnabled)
        message.put("isAudioEnabled", isAudioEnabled)
        signalingSocket?.emit("join_room", message)
    }

    private fun receivedAllUsersMessage(message: JSONArray) {
        Log.d(TAG,"receivedAllUsersMessage")

        var messageJsonArrayLength = message.length()
        var peerConnectionClient: PeerConnectionClient? = null;

        for (i in 0 until messageJsonArrayLength) {1
            var senderSocketID    = (message.get(i) as JSONObject).get("socketID") as String
            var senderUserID          = (message.get(i) as JSONObject).get("userID") as String
            var senderUserDisplayName = (message.get(i) as JSONObject).get("userDisplayName") as String
            var senderUserImageUrl    = (message.get(i) as JSONObject).get("userImageUrl") as String
            var isRemoteVideoEnabled        = (message.get(i) as JSONObject).get("isVideoEnabled") as Boolean
            var isRemoteAudioEnabled        = (message.get(i) as JSONObject).get("isAudioEnabled") as Boolean

            // User 정보 관리
            var user = User(senderUserID, senderUserDisplayName, senderUserImageUrl)
            socketIDToUserPairs.put(senderSocketID, user)

            // Todo. 나중에는 SignalingParameter를 N개로 나눠야한다. 왜냐하면 iceCandidate가 Peer마다 다르기 때문
            // Make iceServers
            val iceServers = ArrayList<PeerConnection.IceServer>()
            val URL = "stun:stun.l.google.com:19302"
            iceServers.add(PeerConnection.IceServer(URL))

            // Make iceCandidates
            val iceCandidates = mutableListOf<IceCandidate>()
            signalingParameters = SignalingParameters(iceServers, true,  iceCandidates)

            // 1. createPeerConnection
            var videoCapturer: VideoCapturer? = null
            if (peerConnectionParameters!!.videoCallEnabled) {
                videoCapturer = createVideoCapturer()
            }
            // Todo. 나중에는 signalingParameters N개로 나눠야한다. 왜냐하면 iceCandidate가 Peer마다 다르기 때문
            peerConnectionClient = PeerConnectionClient(
                applicationContext,
                peerConnectionFactory,
                rootEglBase,
                peerConnectionParameters,
                isRemoteVideoEnabled,
                isRemoteAudioEnabled,
                this
            )

            peerConnectionClient?.createPeerConnection(
                localVideoTrack,
                videoCapturer,
                signalingParameters
            )

            peerConnectionClient?.remoteSocketId = senderSocketID;
            socketIDToPeerConnectionPairs.put(senderSocketID, peerConnectionClient)

            // createOffer성공 시 WebRTCManager:onLocalDescription 콜백 호출
            peerConnectionClient?.createOffer();
        }
    }

    private fun receivedGetOfferMessage(message: JSONObject) {
        Log.d(TAG, "receivedGetOfferMessage")
        var senderSocketID = message.getString("senderSocketID")
        var senderUserID = message.getString("senderUserID")
        var senderUserDisplayName = message.getString("senderUserDisplayName")
        var senderUserImageUrl = message.getString("senderUserImageUrl")
        var isRemoteVideoEnabled = message.getBoolean("isVideoEnabled")
        var isRemoteAudioEnabled = message.getBoolean("isVideoEnabled")

        // User 정보 관리
        var user = User(senderUserID, senderUserDisplayName, senderUserImageUrl)
        socketIDToUserPairs.put(senderSocketID, user)

        // 1. createPeerConnection & SetLocalDescription
        // Make iceServers
        val iceServers = ArrayList<PeerConnection.IceServer>()
        val URL = "stun:stun.l.google.com:19302"
        iceServers.add(PeerConnection.IceServer(URL))

        // Make iceCandidates
        val iceCandidates = mutableListOf<IceCandidate>()
        signalingParameters = SignalingParameters(iceServers, false,  iceCandidates)

        // createPeerConnection
        var videoCapturer: VideoCapturer? = null
        if (peerConnectionParameters!!.videoCallEnabled) {
            videoCapturer = createVideoCapturer()
        }

        // Todo. 나중에는 signalingParameters N개로 나눠야한다. 왜냐하면 iceCandidate가 Peer마다 다르기 때문
        var peerConnectionClient = PeerConnectionClient(
            applicationContext,
            peerConnectionFactory,
            rootEglBase,
            peerConnectionParameters,
            isRemoteVideoEnabled,
            isRemoteAudioEnabled,
            this
        )
        // Todo. RecyclerView item 추가하여 remoteViewRenderer 대신 전달
        peerConnectionClient?.createPeerConnection(
            localVideoTrack,
            videoCapturer,
            signalingParameters
        )
        peerConnectionClient?.remoteSocketId = senderSocketID
        socketIDToPeerConnectionPairs.put(senderSocketID, peerConnectionClient)

        // 2. SetRemoteDescription
        var sdpPair = message.getJSONObject("sdp")
        var description = sdpPair.getString("sdp");
        peerConnectionClient?.setRemoteDescription(
            SessionDescription(SessionDescription.Type.OFFER, description)
        )

        // 3. createAnswer -> setLocalDescription -> sendAnswer
        peerConnectionClient?.createAnswer();
    }

    private fun receivedGetAnswerMessage(message: JSONObject) {
        Log.d(TAG,"receivedGetAnswerMessage")

        var senderSocketID = message.getString("senderSocketID")

        var sdpPair = message.getJSONObject("sdp")
        var description = sdpPair.getString("sdp");

        var peerConnectionClient = this.socketIDToPeerConnectionPairs.get(senderSocketID)
        peerConnectionClient?.setRemoteDescription(
            SessionDescription(SessionDescription.Type.ANSWER, description)
        )
    }

    private fun receivedGetCandidateMessage(message: JSONObject) {
        Log.d(TAG,"receivedGetCandidateMessage")

        var candidateSendID = message.getString("candidateSendID")

        var rootCandidate   = message.getJSONObject("candidate")
        var candidate       = rootCandidate.getString("candidate")
        var sdpMid          = rootCandidate.getString("sdpMid")
        var sdpMLineIndex   = rootCandidate.getInt("sdpMLineIndex")

        val iceCandidate: IceCandidate? = IceCandidate(
            sdpMid,
            sdpMLineIndex,
            candidate
        )
        var peerConnectionClient = socketIDToPeerConnectionPairs?.get(candidateSendID)
        peerConnectionClient?.let {
            peerConnectionClient.addRemoteIceCandidate(iceCandidate)
        }
    }

    private fun receivedUserExitMessage(message: JSONObject) {
        Log.d(TAG,"receivedUserExitMessage")
        var socketID = message.getString("socketID")

        var removedPCClient = this.socketIDToPeerConnectionPairs?.get(socketID)
        var videoSinkRVAdapter = videoSinkRecyclerView.adapter as SurfaceViewAdapter
        var videoSinkRVLayoutManager = videoSinkRecyclerView.layoutManager as GridLayoutManager

        // Remove UserInfo
        if (this.socketIDToUserPairs?.containsKey(socketID)) {
            this.socketIDToUserPairs?.remove(socketID)
        }

        // Remove PeerConnection
        removedPCClient?.let {
            runOnUiThread {
                videoSinkRVAdapter?.removeItem(socketID)
                // 1. SpanCount를 한개 작게 하여 제곱한 수와 현재 화상채팅 videoTrack 수와 비교한다
                var disCountedSpanCount = videoSinkRVLayoutManager?.spanCount - 1
                var powSpanCount = disCountedSpanCount * disCountedSpanCount
                if (videoSinkRVAdapter?.itemCount <= powSpanCount) {
                    // 2. videoTrack 수가 disCountedSpanCount의 제곱수보다 같거나 작은경우, spanCount감소시킨다.
                    videoSinkRVLayoutManager?.spanCount--
                    videoSinkRVAdapter?.changeItemHeight(videoSinkRVLayoutManager?.spanCount)
                }
            }
            removedPCClient?.stopVideoSource()
            removedPCClient?.close()
            this.socketIDToPeerConnectionPairs?.remove(socketID)
        }
    }

    private fun receivedMediaEnabledChangedMessage(message: JSONObject) {
        Log.d(TAG,"receivedMediaEnabledChangedMessage")
        var socketID = message.getString("senderSocketID")
        var type = message.getString("type")
        var isEnabled = message.getBoolean("isEnabled")

        if (socketIDToPeerConnectionPairs.containsKey(socketID)) {
            var targetPeer = socketIDToPeerConnectionPairs.get(socketID)
            if (type == "video") {
                targetPeer?.setIsRemoteVideoEnabled(isEnabled)
            } else if ( type == "audio") {
                targetPeer?.setIsRemoteAudioEnabled(isEnabled)
                // 상대방 비디오/마이크 상태표시 변경
                // Todo. 211119DGLEE - 우선 audio 표시일때만 변경
                var videoSinkRVAdapter = videoSinkRecyclerView.adapter as SurfaceViewAdapter
                runOnUiThread {
                    videoSinkRVAdapter.updateItemIsEnabeld(socketID, type, isEnabled)
                }
            }
        }
        // 상대방 비디오/마이크 상태표시 변경
//        var videoSinkRVAdapter = videoSinkRecyclerView.adapter as SurfaceViewAdapter
//        runOnUiThread {
//            videoSinkRVAdapter.updateItemIsEnabeld(socketID, type, isEnabled)
//        }
    }

    // Send Offer Message To Signaling Server
    fun sendOffer(remoteUserSocketID: String?, sessionDescription: SessionDescription) {
        val message = JSONObject()
        val sdp = JSONObject()
        try {
            sdp.put("type", "offer")
            sdp.put("sdp", sessionDescription.description)
            message.put("sdp", sdp)
            message.put("receiverSocketID", remoteUserSocketID)
            message.put("senderSocketID", signalingSocket?.id())
            message.put("senderUserID", roomConnectionParameters?.userInfo?.userID)
            message.put("senderUserDisplayName", roomConnectionParameters?.userInfo?.userDisplayName)
            message.put("senderUserImageUrl", roomConnectionParameters?.userInfo?.userImageUrl)
            message.put("isVideoEnabled", isVideoEnabled)
            message.put("isAudioEnabled", isAudioEnabled)
            signalingSocket?.emit("offer", message)
        } catch (e: JSONException) {
            e.printStackTrace()
        } catch (e: Exception){
            e.printStackTrace()
        }
    }

    // Send Answer Message To Signaling Server
    fun sendAnswer(remoteUserSocketID: String?, sessionDescription: SessionDescription) {
        val message = JSONObject()
        val sdp = JSONObject()
        try {
            sdp.put("type", "answer")
            sdp.put("sdp", sessionDescription.description)
            message.put("sdp", sdp)
            message.put("receiverSocketID", remoteUserSocketID)
            message.put("senderSocketID", signalingSocket?.id())
            signalingSocket?.emit("answer", message)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun sendMediaEnabledChanged(isEnabled: Boolean, type: String) {
        val message = JSONObject()
        try {
            message.put("senderSocketID", signalingSocket?.id())
            message.put("type", type)
            message.put("isEnabled", isEnabled)
            signalingSocket?.emit("mediaEnabledChanged", message)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    // -----Implementation of PeerConnectionClient.PeerConnectionEvents.---------
    // Send local peer connection SDP and ICE candidates to remote party.
    // All callbacks are invoked from peer connection client looper thread and
    // are routed to UI thread.

    // 211104 DGLEE :
    // Call from SDP Observer onSetSuccess() Callback when setLocalDescription or setRemoteDescription
    override fun onLocalDescription(remoteUserSocketID: String?, desc: SessionDescription) {
        if (signalingSocket != null) {
            if (signalingParameters!!.initiator) {
                sendOffer(remoteUserSocketID, desc)
            } else {
                sendAnswer(remoteUserSocketID, desc)
            }
        }
        if (peerConnectionParameters!!.videoMaxBitrate > 0) {
            Log.d(TAG,"Set video maximum bitrate: " + peerConnectionParameters!!.videoMaxBitrate)

            var peerConnectionClient = socketIDToPeerConnectionPairs.get(remoteUserSocketID)
            peerConnectionClient?.setVideoMaxBitrate(peerConnectionParameters!!.videoMaxBitrate)
        }
    }

    // Invoked in PCObserver.onIceCandidate
    override fun onIceCandidate(remoteUserSocketID: String?, candidate: IceCandidate?) {
        Log.d(TAG, "onIceCandidate")
        signalingSocket?.let {
            candidate?.let {
                val message = JSONObject()

                var jsonObjectCandidate = JSONObject()
                jsonObjectCandidate.put("candidate", candidate.sdp)
                jsonObjectCandidate.put("sdpMid", candidate.sdpMid)
                jsonObjectCandidate.put("sdpMLineIndex", candidate.sdpMLineIndex)

                message.put("candidate", jsonObjectCandidate)
                message.put("candidateSendID", signalingSocket?.id())
                message.put("candidateReceiveID", remoteUserSocketID)
                signalingSocket?.emit("candidate", message)
            }
        }
    }

    // Invoked in PCObserver.onIceCandidatesRemoved
    override fun onIceCandidatesRemoved(candidates: Array<IceCandidate?>?) {
        if (signalingSocket != null) {
            // appRtcClient.sendLocalIceCandidateRemovals(candidates)
        }
    }

    // Invoked in PCObserver.onIceConnected
    override fun onIceConnected() {
        val delta = System.currentTimeMillis() - callStartedTimeMs
        logAndToast("ICE connected, delay=" + delta + "ms")
    }

    // Invoked in PCObserver.onIceConnectionChange
    override fun onIceDisconnected() {
        logAndToast("ICE disconnected")
    }

    // Invoked in PCObserver.onConnected
    override fun onConnected(remoteUserSocketID: String?) {
        val delta = System.currentTimeMillis() - callStartedTimeMs
        logAndToast("DTLS connected, delay=" + delta + "ms")
        callConnected(remoteUserSocketID)
    }
    // Invoked in PCObserver.onDisconnected
    override fun onDisconnected() {}
    // Invoked in PeerConnectionClient.closeInternal
    override fun onPeerConnectionClosed() {}
    // Invoked in PeerConnectionClient.getStats
    override fun onPeerConnectionStatsReady(reports: Array<StatsReport?>?) {}
    // Invoked in PeerConnectionClient.reportError
    override fun onPeerConnectionError(description: String?) {
        reportError(description!!)
    }

    override fun onAddStream(remoteSocketId: String, videoTrack: VideoTrack) {
        try {
            var videoSinkRVAdapter = (videoSinkRecyclerView?.adapter as SurfaceViewAdapter)
            var videoSinkRVLayoutManager = (videoSinkRecyclerView?.layoutManager as GridLayoutManager)

            var targetUser = socketIDToUserPairs.get(remoteSocketId)
            var targetPeerConection = socketIDToPeerConnectionPairs.get(remoteSocketId)

            var newRVItem = SurfaceViewRVItem(
                remoteSocketId,
                targetUser?.userID,
                targetUser?.userDisplayName,
                targetUser?.userImageUrl,
                targetPeerConection?.isRemoteVideoEnabled!!,
                targetPeerConection?.isRemoteAudioEnabled!!,
                videoTrack
            )

            runOnUiThread {
                videoSinkRVAdapter?.addItem(newRVItem)
                // 1. RecyclerView의 가로 칸 수의 제곱보다 유저 화면 갯수가 많아지면 spanCount증가
                var powSpanCount = videoSinkRVLayoutManager?.spanCount * videoSinkRVLayoutManager?.spanCount
                if (videoSinkRVAdapter?.itemCount > powSpanCount) {
                    videoSinkRVLayoutManager?.spanCount++
                    videoSinkRVAdapter?.changeItemHeight(videoSinkRVLayoutManager?.spanCount)
                }
            }
        } catch (e: Exception){
            e.printStackTrace()
        }
    }

    private fun startCall() {
        callStartedTimeMs = System.currentTimeMillis()

        // Start room connection.
        logAndToast(applicationContext.getString(R.string.connecting_to, roomConnectionParameters!!.signalingServerUrl))

        connectToSignalingServer()

        // Create and audio manager that will take care of audio routing,
        // audio modes, audio device enumeration etc.
        audioManager = AppRTCAudioManager.create(applicationContext)

        // Store existing audio settings and change audio mode to
        // MODE_IN_COMMUNICATION for best possible VoIP performance.
        Log.d(TAG, "Starting the audio manager...")

        audioManager?.start(object : AppRTCAudioManager.AudioManagerEvents {
            // This method will be called each time the number of available audio
            // devices has changed.
            override fun onAudioDeviceChanged(
                audioDevice: AppRTCAudioManager.AudioDevice?, availableAudioDevices: Set<AppRTCAudioManager.AudioDevice?>?
            ) {
                onAudioManagerDevicesChanged(audioDevice, availableAudioDevices)
            }
        })
    }

    private fun connectToSignalingServer() {
        try {
            signalingSocket?.on(Socket.EVENT_CONNECT, Emitter.Listener { args: Array<Any?>? ->
                // SocketIO에서 관리하는 Worker Thread
                runOnUiThread {
                    val myToast = Toast.makeText(applicationContext, "Socket.EVENT_CONNECT", Toast.LENGTH_SHORT)
                    myToast.show()
                }
                receivedConnectedEvent()
            })?.on("all_users", Emitter.Listener { args: Array<Any?>? ->
                runOnUiThread {
                    val myToast = Toast.makeText(applicationContext, "receivedAllUsersMessage", Toast.LENGTH_SHORT)
                    myToast.show()
                }
                receivedAllUsersMessage(args?.get(0) as JSONArray)
            })?.on("getOffer", Emitter.Listener { args: Array<Any?>? ->
                runOnUiThread {
                    val myToast = Toast.makeText(applicationContext, "receivedGetOfferMessage", Toast.LENGTH_SHORT)
                    myToast.show()
                }
                receivedGetOfferMessage(args?.get(0) as JSONObject)
            })?.on("getAnswer", Emitter.Listener { args: Array<Any?>? ->
                runOnUiThread {
                    val myToast = Toast.makeText(applicationContext, "receivedGetAnswerMessage", Toast.LENGTH_SHORT)
                    myToast.show()
                }
                receivedGetAnswerMessage(args?.get(0) as JSONObject)
            })?.on("getCandidate", Emitter.Listener { args: Array<Any?>? ->
                runOnUiThread {
                    val myToast = Toast.makeText(applicationContext, "receivedGetCandidateMessage", Toast.LENGTH_SHORT)
                    myToast.show()
                }
                receivedGetCandidateMessage(args?.get(0) as JSONObject)
            })?.on("mediaEnabledChanged", Emitter.Listener { args: Array<Any?>? ->
                runOnUiThread {
                    val myToast = Toast.makeText(applicationContext, "receivedMediaEnabledChanged", Toast.LENGTH_SHORT)
                    myToast.show()
                }
                receivedMediaEnabledChangedMessage(args?.get(0) as JSONObject)
            })?.on("user_exit", Emitter.Listener { args: Array<Any?>? ->
                runOnUiThread {
                    val myToast = Toast.makeText(applicationContext, "receivedUserExit", Toast.LENGTH_SHORT)
                    myToast.show()
                }
                receivedUserExitMessage(args?.get(0) as JSONObject)
            })
            signalingSocket?.connect()
        } catch(e: URISyntaxException){
            e.printStackTrace()
        } catch(e: Exception){
            e.printStackTrace()
        }
    }

    private fun runOnUiThread(runnable: Runnable) {
        handler.post(runnable)
    }

    // Log |msg| and Toast about it.
    private fun logAndToast(msg: String) {
        Log.d(TAG, msg)
        runOnUiThread {
            logToast?.cancel()
            logToast = Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT)
            logToast?.show()
        }
    }

    private fun reportError(description: String) {
        if (!isError) {
            isError = true
            // 211115 - 우선 에러 발생해도 연결 끊지 않음
            //disconnectWithErrorMessage(description)
        }
    }

    private fun disconnectWithErrorMessage(errorMessage: String) {
        runOnUiThread {
            if (!activityRunning) {
                Log.e(TAG, "Critical error: $errorMessage")
                //disconnect()
            } else {
                AlertDialog.Builder(applicationContext!!)
                    .setTitle(applicationContext.getText(R.string.channel_error_title))
                    .setMessage(errorMessage)
                    .setCancelable(false)
                    .setNeutralButton(
                        R.string.ok
                    ) { dialog, id ->
                        dialog.cancel()
                    }
                    .create()
                    .show()
            }
        }
    }
}