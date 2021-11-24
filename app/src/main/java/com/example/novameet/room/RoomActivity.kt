package com.example.novameet.room
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import android.animation.ObjectAnimator
import android.annotation.TargetApi
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.GridLayoutManager
import androidx.transition.Visibility
import com.example.novameet.databinding.ActivityRoomBinding
import com.example.novameet.model.User
import com.example.novameet.R
import com.example.novameet.chat.ChatActivity
import com.example.novameet.home.RoomsRecyclerViewAdapter
import com.example.novameet.member.MemberActivity
import com.example.novameet.network.RetrofitManager
import com.example.novameet.room.WebRTC.*

import com.example.webrtcmultipleandroidsample.WebRTC.WebRTCProperties
import org.webrtc.*

class RoomActivity : AppCompatActivity() {
    private val TAG : String = "RoomActivity"

    private val binding: ActivityRoomBinding by lazy { ActivityRoomBinding.inflate(layoutInflater) }

    private val rootEglBase: EglBase = EglBase.create()

    private val loginUser: User? by lazy { intent.getSerializableExtra("userInfo") as User }
    private val isRoomOwner: Boolean by lazy { intent.getBooleanExtra("isRoomOwner", false) }
    private val roomId: String? by lazy { intent.getStringExtra(WebRTCProperties.EXTRA_ROOMID) }

    private var mainFabIsOpen: Boolean = false

    private var roomConnectionParameters: RoomConnectionParameters? = null
    private var peerConnectionParameters: PeerConnectionParameters? = null

    private var screencaptureEnabled = false

    private var isVideoEnabled = true
    private var isMicEnabled = true

    // List of mandatory application permissions.
    private val MANDATORY_PERMISSIONS = arrayOf(
        "android.permission.MODIFY_AUDIO_SETTINGS",
        "android.permission.RECORD_AUDIO",
        "android.permission.INTERNET"
    )

    // Peer connection statistics callback period in ms.

    private var webRTCManager: WebRTCManager? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        Thread.setDefaultUncaughtExceptionHandler(UnhandledExceptionHandler(this))

        initWindow()
        initFab()
        // 권한 체크, 권한 없을 시 Activity Finish
        checkMandatoryPermissions()

        // Get Intent parameters.
        Log.d(TAG, "Room ID: $roomId")
        if (roomId == null || roomId?.length == 0) {
            Log.e(TAG, "Incorrect room ID in intent!")
            setResult(RESULT_CANCELED)
            finish()
            return
        }


        binding?.recyclerView?.adapter = SurfaceViewAdapter(
            rootEglBase, this.windowManager.defaultDisplay
        )
        binding?.recyclerView?.layoutManager = GridLayoutManager(this, 1)

        initParameters()

        webRTCManager = WebRTCManager(
            applicationContext,
            binding?.recyclerView,
            peerConnectionParameters,
            roomConnectionParameters,
            rootEglBase
        )

        webRTCManager?.setUseCamara2(intent.getBooleanExtra(WebRTCProperties.EXTRA_CAMERA2,true))
        webRTCManager?.setIsCaptureToTexture(intent.getBooleanExtra(WebRTCProperties.EXTRA_CAPTURETOTEXTURE_ENABLED, false))
        webRTCManager?.start()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onDestroy() {
        webRTCManager?.stop();
        super.onDestroy()
    }

    private fun initWindow() {
        // Set window styles for fullscreen-window size. Needs to be done before
        // adding content.
//        window.addFlags(
//            WindowManager.LayoutParams.FLAG_FULLSCREEN or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
//                    or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
//        )
//        window.decorView.systemUiVisibility = getSystemUiVisibility()
    }

    @TargetApi(17)
    private fun getDisplayMetrics(): DisplayMetrics? {
        val displayMetrics = DisplayMetrics()
        val windowManager = application.getSystemService(AppCompatActivity.WINDOW_SERVICE) as WindowManager
        windowManager.defaultDisplay.getRealMetrics(displayMetrics)
        return displayMetrics
    }

    @TargetApi(19)
    private fun getSystemUiVisibility(): Int {
        var flags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            flags = flags or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        }
        return flags
    }

    private fun initFab() {
        if (isRoomOwner) {
            binding.deleteRoomFab.visibility = View.VISIBLE
        }
        binding.mainFab.setOnClickListener {
            if (!mainFabIsOpen) {
                mainFabIsOpen = true
                var margin = -200f
                var movingDistance = margin
                if (isRoomOwner) {
                    ObjectAnimator.ofFloat(binding.deleteRoomFab, "translationY", movingDistance).apply { start() }
                    movingDistance += margin
                }
                ObjectAnimator.ofFloat(binding.callEndFab, "translationY", movingDistance).apply { start() }
                movingDistance += margin
                ObjectAnimator.ofFloat(binding.chatFab, "translationY", movingDistance).apply { start() }
                movingDistance += margin
                ObjectAnimator.ofFloat(binding.micFab, "translationY", movingDistance).apply { start() }
                movingDistance += margin
                ObjectAnimator.ofFloat(binding.videoFab, "translationY", movingDistance).apply { start() }
            } else {
                mainFabIsOpen = false
                ObjectAnimator.ofFloat(binding.deleteRoomFab, "translationY", 0f).apply { start() }
                ObjectAnimator.ofFloat(binding.callEndFab, "translationY", 0f).apply { start() }
                ObjectAnimator.ofFloat(binding.chatFab, "translationY", 0f).apply { start() }
                ObjectAnimator.ofFloat(binding.micFab, "translationY", 0f).apply { start() }
                ObjectAnimator.ofFloat(binding.videoFab, "translationY", 0f).apply { start() }
            }
        }
        binding.deleteRoomFab.setOnClickListener {
            requestDeleteRoom()
        }
        binding.callEndFab.setOnClickListener {
            finish()
        }
        binding.chatFab.setOnClickListener {
            startActivity(Intent(this, ChatActivity::class.java))
        }

        /*
            xml파일에서 android:backgroundTint를 이용하여 초기 배경색을 세팅 할 경우,
            kotlin 코드 내에서 동적으로 배경색을 변경 할 수 없는 현상 발생.
            따라서 코드 내에서 fab의 색상값 초기화
         */
        binding.micFab.backgroundTintList =
            AppCompatResources.getColorStateList(this, R.color.gray_fab)
        binding.micFab.setOnClickListener {
            isMicEnabled = !isMicEnabled
            // 오디오 끄기 and 화상채팅방 참여자들에게 상태변경 메시지 전송
            webRTCManager?.setMicEnabled(isMicEnabled)
            //Floating Action Button 색상/아이콘 변경
            if (isMicEnabled) {
                binding.micFab.setImageResource(R.drawable.ic_baseline_mic_24)
                binding.micFab.backgroundTintList =
                    AppCompatResources.getColorStateList(this, R.color.gray_fab)
            } else {
                binding.micFab.setImageResource(R.drawable.ic_baseline_mic_off_24)
                binding.micFab.backgroundTintList =
                    AppCompatResources.getColorStateList(this, R.color.red_fab)
            }
        }
        /*
            xml파일에서 android:backgroundTint를 이용하여 초기 배경색을 세팅 할 경우,
            kotlin 코드 내에서 동적으로 배경색을 변경 할 수 없는 현상 발생.
            따라서 코드 내에서 fab의 색상값 초기화
         */
        binding.videoFab.backgroundTintList =
            AppCompatResources.getColorStateList(this, R.color.gray_fab)
        binding.videoFab.setOnClickListener {
            isVideoEnabled = !isVideoEnabled
            // 화면 끄기 and 화상채팅방 참여자들에게 상태변경 메시지 전송
            webRTCManager?.setVideoEnabled(isVideoEnabled)
            // Floating Action Button 색상/아이콘 변경
            if (isVideoEnabled) {
                binding.videoFab.setImageResource(R.drawable.ic_baseline_videocam_24)
                binding.videoFab.backgroundTintList =
                    AppCompatResources.getColorStateList(this, R.color.gray_fab)
            } else {
                binding.videoFab.setImageResource(R.drawable.ic_baseline_videocam_off_24)
                binding.videoFab.backgroundTintList =
                    AppCompatResources.getColorStateList(this, R.color.red_fab)
            }
        }
    }

    private fun initParameters() {
        val signalingServerUri = intent.getStringExtra(WebRTCProperties.EXTRA_SIGNALING_URI)
        if (signalingServerUri == null) {
            Log.e(TAG, "Didn't get any URL in intent!")
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        val loopback = intent.getBooleanExtra(WebRTCProperties.EXTRA_LOOPBACK, false)
        val tracing = intent.getBooleanExtra(WebRTCProperties.EXTRA_TRACING, false)

        var videoWidth = intent.getIntExtra(WebRTCProperties.EXTRA_VIDEO_WIDTH, 240)
        var videoHeight = intent.getIntExtra(WebRTCProperties.EXTRA_VIDEO_HEIGHT, 240)

        screencaptureEnabled = intent.getBooleanExtra(WebRTCProperties.EXTRA_SCREENCAPTURE, false)
        // If capturing format is not specified for screencapture, use screen resolution.
        if (screencaptureEnabled && videoWidth == 0 && videoHeight == 0) {
            val displayMetrics = getDisplayMetrics()
            videoWidth  = displayMetrics!!.widthPixels
            videoHeight = displayMetrics.heightPixels
        }
        var dataChannelParameters: DataChannelParameters? = null
        if (intent.getBooleanExtra(WebRTCProperties.EXTRA_DATA_CHANNEL_ENABLED, false)) {
            dataChannelParameters = DataChannelParameters(
                intent.getBooleanExtra(WebRTCProperties.EXTRA_ORDERED, true),
                intent.getIntExtra(WebRTCProperties.EXTRA_MAX_RETRANSMITS_MS, -1),
                intent.getIntExtra(WebRTCProperties.EXTRA_MAX_RETRANSMITS, -1),
                intent.getStringExtra(WebRTCProperties.EXTRA_PROTOCOL),
                intent.getBooleanExtra(WebRTCProperties.EXTRA_NEGOTIATED, false),
                intent.getIntExtra(WebRTCProperties.EXTRA_ID, -1)
            )
        }

        peerConnectionParameters = PeerConnectionParameters(
            intent.getBooleanExtra(WebRTCProperties.EXTRA_VIDEO_CALL, true),
            loopback,
            tracing,
            videoWidth,
            videoHeight,
            intent.getIntExtra(WebRTCProperties.EXTRA_VIDEO_FPS, 0),
            intent.getIntExtra(WebRTCProperties.EXTRA_VIDEO_BITRATE, 0),
            intent.getStringExtra(WebRTCProperties.EXTRA_VIDEOCODEC),
            intent.getBooleanExtra(WebRTCProperties.EXTRA_HWCODEC_ENABLED, true),
            intent.getBooleanExtra(WebRTCProperties.EXTRA_FLEXFEC_ENABLED, false),
            intent.getIntExtra(WebRTCProperties.EXTRA_AUDIO_BITRATE, 0),
            intent.getStringExtra(WebRTCProperties.EXTRA_AUDIOCODEC),
            intent.getBooleanExtra(WebRTCProperties.EXTRA_NOAUDIOPROCESSING_ENABLED, false),
            intent.getBooleanExtra(WebRTCProperties.EXTRA_AECDUMP_ENABLED, false),
            intent.getBooleanExtra(WebRTCProperties.EXTRA_SAVE_INPUT_AUDIO_TO_FILE_ENABLED, false),
            intent.getBooleanExtra(WebRTCProperties.EXTRA_OPENSLES_ENABLED, false),
            intent.getBooleanExtra(WebRTCProperties.EXTRA_DISABLE_BUILT_IN_AEC, false),
            intent.getBooleanExtra(WebRTCProperties.EXTRA_DISABLE_BUILT_IN_AGC, false),
            intent.getBooleanExtra(WebRTCProperties.EXTRA_DISABLE_BUILT_IN_NS, false),
            intent.getBooleanExtra(WebRTCProperties.EXTRA_DISABLE_WEBRTC_AGC_AND_HPF, false),
            intent.getBooleanExtra(WebRTCProperties.EXTRA_ENABLE_RTCEVENTLOG, false),
            dataChannelParameters
        )

        // Create connection parameters.
        roomConnectionParameters = RoomConnectionParameters(
            signalingServerUri,
            loginUser,
            roomId,
            isRoomOwner,
            loopback,
        )
    }

    private fun checkMandatoryPermissions() {
        for (permission in MANDATORY_PERMISSIONS) {
            if (checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                setResult(RESULT_CANCELED)
                finish()
                return
            }
        }
    }

    private fun requestDeleteRoom() {
        // 1. WAS에게 DB 삭제 요청
        RetrofitManager.instance.requestDeleteRoom(roomId = roomId ,completion = { deleteRoomResponse ->
            if (deleteRoomResponse.responseCode == 1) {
                Log.d(TAG, "requestDeleteRoom: 방 제거 성공")
            } else if (deleteRoomResponse.responseCode == 0) {
                Log.d(TAG, "requestDeleteRoom: 방 제거 성공")
            } else {
                Log.d(TAG, "requestDeleteRoom: 방 목록 없음")
            }
        })

        // 2. Chat Server에게 leave_all 요청
        // gChatSocket.emit('leave_all');
    }
}