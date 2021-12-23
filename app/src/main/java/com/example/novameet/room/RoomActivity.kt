package com.example.novameet.room

import android.animation.ObjectAnimator
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.GridLayoutManager
import com.example.novameet.databinding.ActivityRoomBinding
import com.example.novameet.model.User
import com.example.novameet.R
import com.example.novameet.room.chat.ChatActivity
import com.example.novameet.network.RetrofitManager
import com.example.novameet.room.WebRTC.*

import com.example.webrtcmultipleandroidsample.WebRTC.WebRTCProperties
import org.webrtc.*

class RoomActivity : AppCompatActivity(), RecordBottomSheetDlgEvent {
    private val TAG : String = "RoomActivity"

    private val binding: ActivityRoomBinding by lazy { ActivityRoomBinding.inflate(layoutInflater) }

    private val rootEglBase: EglBase = EglBase.create()

    private val loginUser: User? by lazy { intent.getSerializableExtra("userInfo") as User }
    private val isRoomOwner: Boolean by lazy { intent.getBooleanExtra("isRoomOwner", false) }
    private val roomId: String? by lazy { intent.getStringExtra(WebRTCProperties.EXTRA_ROOMID) }

    private var mainFabIsOpen: Boolean = false

    private var isVideoEnabled = true
    private var isMicEnabled = true

    private var recordBottomSheetDlg: RecordBottomSheetDlg? = RecordBottomSheetDlg(this)

    // region Bind Service
    private val binderExtendedServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            roomServiceBinder = service as RoomService.RoomServiceBinder

            roomServiceBinder?.service?.setUserInfo(loginUser)
            roomServiceBinder?.service?.setRecordBottomSheetDlgEvent(this@RoomActivity)

            roomServiceBinder?.service?.setWebRTCManager(intent, rootEglBase, binding.recyclerView)
            roomServiceBinder?.service?.startWebRTCManager()

            roomServiceBinder?.service?.setChatManager(roomId)
            roomServiceBinder?.service?.startChatManager()

            val myToast = Toast.makeText(applicationContext,
                "BinderExtendedService - onServiceConnected",
                Toast.LENGTH_SHORT)
            myToast.show()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            roomServiceBinder = null

            val myToast = Toast.makeText(applicationContext,
                "BinderExtendedService - onServiceDisconnected",
                Toast.LENGTH_SHORT)
            myToast.show()
        }
    }
    private var roomServiceBinder: RoomService.RoomServiceBinder? = null
    // endregion

    // List of mandatory application permissions.
    private val MANDATORY_PERMISSIONS = arrayOf(
        "android.permission.MODIFY_AUDIO_SETTINGS",
        "android.permission.RECORD_AUDIO",
        "android.permission.INTERNET"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        Thread.setDefaultUncaughtExceptionHandler(UnhandledExceptionHandler(this))

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

        val signalingServerUri = intent.getStringExtra(WebRTCProperties.EXTRA_SIGNALING_URI)
        if (signalingServerUri == null) {
            Log.e(TAG, "Didn't get any URL in intent!")
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        initWindow()
        initFab()

        binding?.recyclerView?.adapter = SurfaceViewAdapter(
            rootEglBase, this.windowManager.defaultDisplay
        )
        binding?.recyclerView?.layoutManager = GridLayoutManager(this, 1)

        startRoomService()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onBackPressed() {
        setRoomActivityResult()
        super.onBackPressed()
    }

    private fun setRoomActivityResult() {
        var resultIntent = Intent()
        resultIntent.putExtra("userInfo", loginUser)
        setResult(RESULT_OK, resultIntent)
    }

    private fun startRoomService() {
        Intent(this, RoomService::class.java).run {
            bindService(this, binderExtendedServiceConnection, Service.BIND_AUTO_CREATE)
        }
    }

    private fun stopRoomService() {
        unbindService(binderExtendedServiceConnection)
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
                ObjectAnimator.ofFloat(binding.recordFab, "translationY", movingDistance).apply { start() }
                movingDistance += margin
                ObjectAnimator.ofFloat(binding.loggedInUsersFab, "translationY", movingDistance).apply { start() }
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
                ObjectAnimator.ofFloat(binding.recordFab, "translationY", 0f).apply { start() }
                ObjectAnimator.ofFloat(binding.loggedInUsersFab, "translationY", 0f).apply { start() }
                ObjectAnimator.ofFloat(binding.chatFab, "translationY", 0f).apply { start() }
                ObjectAnimator.ofFloat(binding.micFab, "translationY", 0f).apply { start() }
                ObjectAnimator.ofFloat(binding.videoFab, "translationY", 0f).apply { start() }
            }
        }
        binding.deleteRoomFab.setOnClickListener {
            requestDeleteRoom()
        }
        binding.callEndFab.setOnClickListener {
            stopRoomService()
            setRoomActivityResult()
            finish()
        }
        binding.chatFab.setOnClickListener {
            var intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("loginUserID", loginUser?.userID)
            startActivity(intent)
        }
        binding.loggedInUsersFab.setOnClickListener {
            var intent = Intent(this, LoggedInUsersActivity::class.java)
            intent.putExtra("loginUserID", loginUser?.userID)
            intent.putExtra("roomID", roomId)
            startActivity(intent)
        }
        binding.recordFab.setOnClickListener {
            this.recordBottomSheetDlg?.show(supportFragmentManager, TAG)
            // 집중시간 기록 다이얼로그에 표시되는 집중시간 값 초기화
            recordBottomSheetDlg?.updateFocusTimeView(loginUser?.dailyFocusTime ?: 0)
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
            roomServiceBinder?.service?.setMicEnabled(isMicEnabled)
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
            roomServiceBinder?.service?.setVideoEnabled(isVideoEnabled)
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
        // 응답 받았을 때 stopRoomService
    }

    override fun onDlgCreateView() {
        this.recordBottomSheetDlg?.updateFocusTimeView(loginUser?.dailyFocusTime ?: 0)
    }

    override fun onRecordStartButtonClicked() {
        this.roomServiceBinder?.service?.startFocusTimer()
    }

    override fun onRecordStopButtonClicked() {
        this.roomServiceBinder?.service?.stopFocusTimer()
    }

    override fun onUpdatedFocusTime(focusTime: Int) {
        this.recordBottomSheetDlg?.updateFocusTimeView(focusTime)
    }
}