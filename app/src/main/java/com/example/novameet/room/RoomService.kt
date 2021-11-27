package com.example.novameet.room

import android.annotation.TargetApi
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.os.Binder
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.annotation.IntRange
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.example.novameet.R
import com.example.novameet.model.User
import com.example.novameet.room.WebRTC.DataChannelParameters
import com.example.novameet.room.WebRTC.PeerConnectionParameters
import com.example.novameet.room.WebRTC.RoomConnectionParameters
import com.example.novameet.room.chat.ChatManager
import com.example.webrtcmultipleandroidsample.WebRTC.WebRTCProperties
import org.webrtc.EglBase
import kotlin.concurrent.thread

class RoomService : Service() {

    // For WebRTC
    private var roomConnectionParameters: RoomConnectionParameters? = null
    private var peerConnectionParameters: PeerConnectionParameters? = null
    private var screencaptureEnabled = false
    private var webRTCManager: WebRTCManager? = null

    // For Chat
    var chatManager: ChatManager? = null

    override fun onBind(intent: Intent): IBinder {
        return RoomServiceBinder()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        stopWebRTCManager()
        stopChatManager()
        super.onDestroy()
    }

    fun setWebRTCManager(intent: Intent, rootEglBase: EglBase, recyclerView: RecyclerView) {
        initParameters(intent)

        webRTCManager = WebRTCManager(
            applicationContext,
            recyclerView,
            peerConnectionParameters,
            roomConnectionParameters,
            rootEglBase
        )

        webRTCManager?.setUseCamara2(intent.getBooleanExtra(WebRTCProperties.EXTRA_CAMERA2,true))
        webRTCManager?.setIsCaptureToTexture(intent.getBooleanExtra(WebRTCProperties.EXTRA_CAPTURETOTEXTURE_ENABLED, false))
    }

    fun startWebRTCManager() {
        webRTCManager?.start()
    }

    fun stopWebRTCManager () {
        webRTCManager?.stop()
    }

    fun setChatManager(userInfo: User?, roomID: String?) {
        chatManager = ChatManager(
            userInfo?.userID,
            userInfo?.userDisplayName,
            userInfo?.userImageUrl,
            roomID
        )
    }

    fun startChatManager() {
        chatManager?.start()
    }

    fun stopChatManager() {
        chatManager?.stop()
    }

    fun setVideoEnabled(isEnabled: Boolean) {
        webRTCManager?.setVideoEnabled(isEnabled)
    }

    fun setMicEnabled(isEnabled: Boolean) {
        webRTCManager?.setMicEnabled(isEnabled)
    }

    private fun initParameters(intent: Intent) {
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
            intent.getStringExtra(WebRTCProperties.EXTRA_SIGNALING_URI),
            intent.getSerializableExtra("userInfo") as User,
            intent.getStringExtra(WebRTCProperties.EXTRA_ROOMID),
            intent.getBooleanExtra("isRoomOwner", false),
            loopback,
        )
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

    inner class RoomServiceBinder : Binder() {
        val service: RoomService
            get() = this@RoomService
    }
}