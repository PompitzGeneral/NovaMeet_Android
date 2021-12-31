package com.example.novameet
import android.content.Context
import androidx.preference.PreferenceManager
import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast

import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.EditText
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.novameet.databinding.ActivityMainBinding
import com.example.novameet.home.HomeFragment
import com.example.novameet.login.LoginActivity
import com.example.novameet.model.User
import com.example.novameet.network.RetrofitManager
import com.example.novameet.profile.ProfileActivity
import com.example.novameet.record.RecordFragment
import com.example.novameet.room.RoomActivity
import com.example.novameet.room.RoomCreateActivity
import com.example.webrtcmultipleandroidsample.WebRTC.WebRTCProperties
import org.json.JSONArray
import org.json.JSONException
import pub.devrel.easypermissions.EasyPermissions
import java.lang.NumberFormatException
import java.util.ArrayList

import android.widget.LinearLayout

import android.view.LayoutInflater
import com.example.novameet.network.RetrofitClient
import android.webkit.CookieManager
import com.example.novameet.login.AutoLogin
import com.example.novameet.pushmessage.PushMessageService
import okhttp3.internal.notifyAll


class MainActivity : AppCompatActivity() {

    private val TAG : String = "[MainActivity]"
    private val RC_CALL = 111
    private val CONNECTION_REQUEST = 1
    private val PERMISSION_REQUEST = 2

    private val binding: ActivityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val homeFragment by lazy { HomeFragment() }
    private val recordFragment by lazy { RecordFragment() }
    private var bottomNavSelectedId = R.id.navigation_home;
    private var isLoggedIn = false;
    private var user: User? = null;

    private var pushMessageServiceIntent: Intent? = null

    // build.gradle에 ' implementation "androidx.preference:preference-ktx:1.1.+" ' 추가
    private fun getSharedPref(context: Context): SharedPreferences? {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }
    //For RoomActivity
    private val sharedPref: SharedPreferences? by lazy { PreferenceManager.getDefaultSharedPreferences(this) }
    private val keyprefResolution: String? by lazy { getString(R.string.pref_resolution_key) }
    private val keyprefFps: String? by lazy { getString(R.string.pref_fps_key) }
    private val keyprefVideoBitrateType: String? by lazy { getString(R.string.pref_maxvideobitrate_key) }
    private val keyprefVideoBitrateValue: String? by lazy { getString(R.string.pref_maxvideobitratevalue_key) }
    private val keyprefAudioBitrateType: String? by lazy { getString(R.string.pref_startaudiobitrate_key) }
    private val keyprefAudioBitrateValue: String? by lazy { getString(R.string.pref_startaudiobitratevalue_key) }
    private val keyprefRoomServerUrl: String? by lazy { getString(R.string.pref_room_server_url_key) }

    val startForLoginResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val intent = result.data

            val userIdx: Int = intent?.extras?.getInt("userIdx") ?: 0;
            val userID: String? = intent?.extras?.getString("userEmail");
            val userDisplayName: String? = intent?.extras?.getString("userDisplayName");
            val userImageUrl: String? = intent?.extras?.getString("userImageUrl");
            val dailyFocusTime: Int = intent?.extras?.getInt("dailyFocusTime") ?: 0;

            setLoggedInStatus(userIdx, userID, userDisplayName, userImageUrl, dailyFocusTime)

            // Immortal 서비스 종료 시킨 후 자동 재시작되며, Shared에서 유저 정보 가져온 후 Join 시도
            stopPushMessageService()
        }
    }

    val startForProfileResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val intent = result.data
            val userDisplayName: String? = intent?.extras?.getString("userDisplayName");
            val userImageUrl: String? = intent?.extras?.getString("userImageUrl");

            user?.userDisplayName = userDisplayName;
            user?.userImageUrl = userImageUrl;

            setProfileImage(userImageUrl)
        }
    }

    val startForCreateRoomResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val roomId: String? = result.data?.extras?.getString("roomId")

            showRoomActivity(roomId, true)
        }
    }

    val startForEnterRoomResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val intent = result.data
            val loginUser: User? = intent?.extras?.getSerializable("userInfo") as User
            loginUser?.let {
                this.user = loginUser
            }
        }
    }

    private fun setLoggedInStatus(userIdx:Int, userID:String?, userDisplayName:String?, userImageUrl:String?, dailyFocusTime:Int) {
        user = User(
            userIdx = userIdx,
            userID = userID,
            userDisplayName = userDisplayName,
            userImageUrl = userImageUrl,
            dailyFocusTime = dailyFocusTime
        )

        isLoggedIn = true;

        setProfileImage(userImageUrl)

        refreshVisiblityToolbarMenu()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        homeFragment?.setRoomClickCallback { roomId, hasPassword ->
            enterRoom(roomId, hasPassword)
        }

        requestPermissions()
        // 데이터 복구
        savedInstanceState?.let {
            bottomNavSelectedId = it.getInt("bottomNavSelectedId")
            isLoggedIn = it.getBoolean("isLoggedIn")
            user = it.getSerializable("userInfo") as User?
        }

        if (AutoLogin.getUserId(this)?.length!! != 0) {
            // Auto Login
            user = User(
                userIdx = AutoLogin.getUserIdx(this),
                userID = AutoLogin.getUserId(this),
                userDisplayName = AutoLogin.getUserDisplayName(this),
                userImageUrl = AutoLogin.getUserImageUrl(this),
                dailyFocusTime = AutoLogin.getDailyFocusTime(this)
            )

            isLoggedIn = true;
        }

        initAppbar()
        initBottomNavigation()

        if (isLoggedIn) {
            user?.let {
                setProfileImage(user?.userImageUrl)
            }
        }

        refreshVisiblityToolbarMenu()

        startPushMessageService()

        RetrofitManager.instance.setContext(applicationContext)

        var fromNoti = intent.getBooleanExtra("isEnteredFromNotification", false)
        if (fromNoti) {
            var roomID = intent.getStringExtra("roomID")
            // Todo. check hasPassword
            enterRoom(roomID, true)
            // fase로 두지 않으면, 이 후에 메인화면 진입 시 계속 enterRoom 메서드 호출한다
            intent.putExtra("isEnteredFromNotification", false)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        stopPushMessageService()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putInt("bottomNavSelectedId", bottomNavSelectedId)
        outState.putBoolean("isLoggedIn", isLoggedIn)
        outState.putSerializable("userInfo", user)
    }

    private fun initAppbar() {
        // Todo. 탐색창 팝업
        binding.imageButtonSearch.setOnClickListener { }
        binding.imageButtonLogin.setOnClickListener { doLoginActivityForResult() }
        binding.imageButtonLogout.setOnClickListener { requestLogout() }
        binding.imageviewUserImage.setOnClickListener { doProfileActivityForResult() }

        refreshVisiblityToolbarMenu()
    }

    private fun changeBottomNavView(selectedItemId: Int) {
        when (selectedItemId) {
            R.id.navigation_home -> {
                // Home Fragment에서는 Activity 세로 방향으로 고정
                try {
                    // API 26 에서만 "Only fullscreen opaque activities can request orientation" 에러 발생
                    // 출처: https://gun0912.tistory.com/79 [박상권의 삽질블로그]
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                } catch (e: Exception){
                    e.printStackTrace()
                }
                replaceFragment(homeFragment)
            }
            R.id.navigation_room_create -> {
                if (isLoggedIn) {
                    doCreateRoomActivityForResult()
                } else {
                    val myToast = Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT)
                    myToast.show()
                }
            }
            R.id.navigation_record -> {
                if (user != null) {
                    // Home Fragment에서는 Activity 가로 방향으로 고정
                    try {
                        // API 26 에서만 "Only fullscreen opaque activities can request orientation" 에러 발생
                        // 출처: https://gun0912.tistory.com/79 [박상권의 삽질블로그]
                        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    } catch (e: Exception){
                        e.printStackTrace()
                    }
                    replaceFragment(recordFragment)
                } else {
                    val myToast = Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT)
                    myToast.show()
                }
            }
        }
    }

    private fun initBottomNavigation() {
        binding.bottomNavView.selectedItemId = this.bottomNavSelectedId

        changeBottomNavView(this.bottomNavSelectedId)

        binding.bottomNavView.setOnNavigationItemSelectedListener { item ->
            this.bottomNavSelectedId = item.itemId;
            changeBottomNavView(item.itemId)

            true
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.nav_host_fragment_activity_main, fragment)
            .commit()
    }

    private fun refreshVisiblityToolbarMenu() {
        binding.imageButtonLogin.isVisible   = !isLoggedIn
        binding.imageButtonLogout.isVisible  = isLoggedIn
        binding.imageviewUserImage.isVisible = isLoggedIn
    }

    fun setProfileImage(uri: String?){
        val userImageView = binding.imageviewUserImage
        try {
            if (!uri.isNullOrEmpty()) {
                userImageView?.let {
                    Glide.with(this)?.load(uri)?.apply(RequestOptions()?.circleCrop()).into(it)
                }
            }
        }catch(e: Exception){
            e.printStackTrace()
            Log.d(TAG, e.toString())
        }
    }

    private fun requestLogout() {
        RetrofitManager.instance.requestLogout(completion = { logoutResponse ->
            if (logoutResponse.responseCode == 1) {
                isLoggedIn = false
                user = null

                refreshVisiblityToolbarMenu()

                AutoLogin.clearUserInfo(this)

                // Immortal 서비스 종료 시킨 후 자동 재시작되며, Shared에서 유저 정보 가져온 후 Join 시도
                stopPushMessageService()

                val resultToast = Toast.makeText(this, "로그아웃 성공", Toast.LENGTH_SHORT)
                resultToast.show()
            } else {
                val resultToast = Toast.makeText(this, "로그아웃 실패", Toast.LENGTH_SHORT)
                resultToast.show()
            }
        })
    }

    private fun doLoginActivityForResult() {
        startForLoginResult.launch(Intent(this, LoginActivity::class.java))
    }

    private fun doProfileActivityForResult() {
        val intent: Intent = Intent(this, ProfileActivity::class.java);
        intent.putExtra("userEmail", user?.userID ?: "");
        intent.putExtra("userDisplayName", user?.userDisplayName ?: "");
        intent.putExtra("userImageUrl", user?.userImageUrl ?: "");
        startForProfileResult.launch(intent)
    }

    private fun doCreateRoomActivityForResult() {
        val intent: Intent = Intent(this, RoomCreateActivity::class.java);
        intent.putExtra("roomOwner", user?.userID ?: "");
        startForCreateRoomResult.launch(intent)
    }

    // HomeFragment
    private fun enterRoom(roomId: String?, hasPassword: Boolean) {
        if (user != null) {
            if (hasPassword) {
                showRoomPasswordDlg(roomId)
            } else {
                requestJoinRoom(user?.userID, roomId, null)
            }
        } else {
            val myToast = Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT)
            myToast.show()
        }
    }

    private fun showRoomPasswordDlg(roomId: String?) {
        val vi = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val layout = vi.inflate(R.layout.dialog_room_password, null) as LinearLayout
        val edittext = layout.findViewById<EditText>(R.id.dlg_password)

        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle("패스워드 입력")
        builder.setView(layout)
        builder.setPositiveButton("확인",
            DialogInterface.OnClickListener { dialog, which ->
                var inputString: String? = edittext.text.toString()
                requestJoinRoom(user?.userID, roomId, inputString)
            })
        builder.setNegativeButton("취소",
            DialogInterface.OnClickListener { dialog, which ->
            })

        var dlg = builder.create()

        dlg.show()
        dlg.window?.setLayout(800, 500)
    }

    private fun requestJoinRoom(userId: String?, roomId: String?, inputPassword: String?) {
        RetrofitManager.instance.requestJoinRoom(
            userId=userId,
            roomId=roomId,
            inputPassword=inputPassword,
            completion = { loginResponse ->
                if (loginResponse.responseCode == 2) {
                    showRoomActivity(roomId, true)
                } else if (loginResponse.responseCode == 1) {
                    showRoomActivity(roomId, false)
                } else if (loginResponse.responseCode == 0) {
                    val myToast = Toast.makeText(this, "참여 가능한 인원수가 다 찼습니다.", Toast.LENGTH_SHORT)
                    myToast.show()
                } else if (loginResponse.responseCode == -1) {
                    val myToast = Toast.makeText(this, "패스워드가 틀렸습니다", Toast.LENGTH_SHORT)
                    myToast.show()
                } else if (loginResponse.responseCode == -2) {
                    val myToast = Toast.makeText(this, "패스워드가 틀렸습니다", Toast.LENGTH_SHORT)
                    myToast.show()
                } else if (loginResponse.responseCode == -3) {
                    val myToast = Toast.makeText(this, "방이 존재하지 않습니다.", Toast.LENGTH_SHORT)
                    myToast.show()
                } else {
                    val myToast = Toast.makeText(this, "예외 케이스. Server Log 확인 필요", Toast.LENGTH_SHORT)
                    myToast.show()
                }
            })
    }

    private fun showRoomActivity(roomId: String?, isRoomOwner: Boolean) {
        // Get default uri.
        val uri: String? =
            sharedPref!!.getString(getString(R.string.pref_signalingserver_uri), getString(R.string.pref_signalingserver_uri))

        // Video call enabled flag.
        val videoCallEnabled: Boolean? = sharedPrefGetBoolean(
            R.string.pref_videocall_key,
            WebRTCProperties.EXTRA_VIDEO_CALL,
            R.string.pref_videocall_default
        )

        // Use screencapture option.
        val useScreencapture: Boolean = sharedPrefGetBoolean(
            R.string.pref_screencapture_key,
            WebRTCProperties.EXTRA_SCREENCAPTURE,
            R.string.pref_screencapture_default
        )

        // Use Camera2 option.
        val useCamera2: Boolean = sharedPrefGetBoolean(
            R.string.pref_camera2_key, WebRTCProperties.EXTRA_CAMERA2,
            R.string.pref_camera2_default
        )
        // Get default codecs.
        val videoCodec: String? = sharedPrefGetString(
            R.string.pref_videocodec_key,
            WebRTCProperties.EXTRA_VIDEOCODEC, R.string.pref_videocodec_default
        )
        val audioCodec: String? = sharedPrefGetString(
            R.string.pref_audiocodec_key,
            WebRTCProperties.EXTRA_AUDIOCODEC, R.string.pref_audiocodec_default
        )
        // Check HW codec flag.
        val hwCodec: Boolean = sharedPrefGetBoolean(
            R.string.pref_hwcodec_key,
            WebRTCProperties.EXTRA_HWCODEC_ENABLED, R.string.pref_hwcodec_default
        )
        // Check Capture to texture.
        val captureToTexture: Boolean = sharedPrefGetBoolean(
            R.string.pref_capturetotexture_key,
            WebRTCProperties.EXTRA_CAPTURETOTEXTURE_ENABLED,
            R.string.pref_capturetotexture_default
        )
        // Check FlexFEC.
        val flexfecEnabled: Boolean = sharedPrefGetBoolean(
            R.string.pref_flexfec_key,
            WebRTCProperties.EXTRA_FLEXFEC_ENABLED,
            R.string.pref_flexfec_default
        )
        // Check Disable Audio Processing flag.
        val noAudioProcessing: Boolean = sharedPrefGetBoolean(
            R.string.pref_noaudioprocessing_key,
            WebRTCProperties.EXTRA_NOAUDIOPROCESSING_ENABLED,
            R.string.pref_noaudioprocessing_default
        )
        val aecDump: Boolean = sharedPrefGetBoolean(
            R.string.pref_aecdump_key,
            WebRTCProperties.EXTRA_AECDUMP_ENABLED,
            R.string.pref_aecdump_default
        )
        val saveInputAudioToFile: Boolean = sharedPrefGetBoolean(
            R.string.pref_enable_save_input_audio_to_file_key,
            WebRTCProperties.EXTRA_SAVE_INPUT_AUDIO_TO_FILE_ENABLED,
            R.string.pref_enable_save_input_audio_to_file_default
        )
        // Check OpenSL ES enabled flag.
        val useOpenSLES: Boolean = sharedPrefGetBoolean(
            R.string.pref_opensles_key,
            WebRTCProperties.EXTRA_OPENSLES_ENABLED, R.string.pref_opensles_default
        )
        // Check Disable built-in AEC flag.
        val disableBuiltInAEC: Boolean = sharedPrefGetBoolean(
            R.string.pref_disable_built_in_aec_key,
            WebRTCProperties.EXTRA_DISABLE_BUILT_IN_AEC,
            R.string.pref_disable_built_in_aec_default
        )
        // Check Disable built-in AGC flag.
        val disableBuiltInAGC: Boolean = sharedPrefGetBoolean(
            R.string.pref_disable_built_in_agc_key,
            WebRTCProperties.EXTRA_DISABLE_BUILT_IN_AGC,
            R.string.pref_disable_built_in_agc_default
        )
        // Check Disable built-in NS flag.
        val disableBuiltInNS: Boolean = sharedPrefGetBoolean(
            R.string.pref_disable_built_in_ns_key,
            WebRTCProperties.EXTRA_DISABLE_BUILT_IN_NS,
            R.string.pref_disable_built_in_ns_default
        )
        // Check Disable gain control
        val disableWebRtcAGCAndHPF: Boolean = sharedPrefGetBoolean(
            R.string.pref_disable_webrtc_agc_and_hpf_key,
            WebRTCProperties.EXTRA_DISABLE_WEBRTC_AGC_AND_HPF,
            R.string.pref_disable_webrtc_agc_and_hpf_key
        )
        // Get video resolution from settings.
        var videoWidth = 0
        var videoHeight = 0

        if (videoWidth == 0 && videoHeight == 0) {
            val resolution = sharedPref!!.getString(
                keyprefResolution,
                getString(R.string.pref_resolution_default)
            )
            val dimensions = resolution!!.split("[ x]+").toTypedArray()
            if (dimensions.size == 2) {
                try {
                    videoWidth = dimensions[0].toInt()
                    videoHeight = dimensions[1].toInt()
                } catch (e: NumberFormatException) {
                    videoWidth = 0
                    videoHeight = 0
                    Log.e(
                        TAG,
                        "Wrong video resolution setting: $resolution"
                    )
                }
            }
        }
        // Get camera fps from settings.
        var cameraFps = 0
        if (cameraFps == 0) {
            val fps = sharedPref!!.getString(keyprefFps, getString(R.string.pref_fps_default))
            val fpsValues = fps!!.split("[ x]+").toTypedArray()
            if (fpsValues.size == 2) {
                try {
                    cameraFps = fpsValues[0].toInt()
                } catch (e: NumberFormatException) {
                    cameraFps = 0
                    Log.e(TAG, "Wrong camera fps setting: $fps")
                }
            }
        }
        // Check capture quality slider flag.
        val captureQualitySlider: Boolean = sharedPrefGetBoolean(
            R.string.pref_capturequalityslider_key,
            WebRTCProperties.EXTRA_VIDEO_CAPTUREQUALITYSLIDER_ENABLED,
            R.string.pref_capturequalityslider_default
        )
        // Get video and audio start bitrate.
        var videoStartBitrate = 0
        if (videoStartBitrate == 0) {
            val bitrateTypeDefault = getString(R.string.pref_maxvideobitrate_default)
            val bitrateType = sharedPref!!.getString(keyprefVideoBitrateType, bitrateTypeDefault)
            if (bitrateType != bitrateTypeDefault) {
                val bitrateValue = sharedPref!!.getString(
                    keyprefVideoBitrateValue, getString(R.string.pref_maxvideobitratevalue_default)
                )
                videoStartBitrate = bitrateValue!!.toInt()
            }
        }
        var audioStartBitrate = 0
        if (audioStartBitrate == 0) {
            val bitrateTypeDefault = getString(R.string.pref_startaudiobitrate_default)
            val bitrateType = sharedPref!!.getString(keyprefAudioBitrateType, bitrateTypeDefault)
            if (bitrateType != bitrateTypeDefault) {
                val bitrateValue = sharedPref!!.getString(
                    keyprefAudioBitrateValue,
                    getString(R.string.pref_startaudiobitratevalue_default)
                )
                audioStartBitrate = bitrateValue!!.toInt()
            }
        }

        // Check statistics display option.
        val displayHud: Boolean = sharedPrefGetBoolean(
            R.string.pref_displayhud_key,
            WebRTCProperties.EXTRA_DISPLAY_HUD, R.string.pref_displayhud_default
        )
        val tracing: Boolean = sharedPrefGetBoolean(
            R.string.pref_tracing_key, WebRTCProperties.EXTRA_TRACING,
            R.string.pref_tracing_default
        )
        // Check Enable RtcEventLog.
        val rtcEventLogEnabled: Boolean = sharedPrefGetBoolean(
            R.string.pref_enable_rtceventlog_key,
            WebRTCProperties.EXTRA_ENABLE_RTCEVENTLOG, R.string.pref_enable_rtceventlog_default
        )
        // Get datachannel options
        val dataChannelEnabled: Boolean = sharedPrefGetBoolean(
            R.string.pref_enable_datachannel_key,
            WebRTCProperties.EXTRA_DATA_CHANNEL_ENABLED, R.string.pref_enable_datachannel_default
        )
        val ordered: Boolean = sharedPrefGetBoolean(
            R.string.pref_ordered_key,
            WebRTCProperties.EXTRA_ORDERED,
            R.string.pref_ordered_default
        )
        val negotiated: Boolean = sharedPrefGetBoolean(
            R.string.pref_negotiated_key,
            WebRTCProperties.EXTRA_NEGOTIATED,
            R.string.pref_negotiated_default
        )
        val maxRetrMs: Int = sharedPrefGetInteger(
            R.string.pref_max_retransmit_time_ms_key,
            WebRTCProperties.EXTRA_MAX_RETRANSMITS_MS, R.string.pref_max_retransmit_time_ms_default
        )
        val maxRetr: Int = sharedPrefGetInteger(
            R.string.pref_max_retransmits_key,
            WebRTCProperties.EXTRA_MAX_RETRANSMITS,
            R.string.pref_max_retransmits_default
        )
        val id: Int = sharedPrefGetInteger(
            R.string.pref_data_id_key,
            WebRTCProperties.EXTRA_ID,
            R.string.pref_data_id_default
        )
        val protocol: String? = sharedPrefGetString(
            R.string.pref_data_protocol_key,
            WebRTCProperties.EXTRA_PROTOCOL,
            R.string.pref_data_protocol_default
        )

        val intent = Intent(this, RoomActivity::class.java)
        intent.putExtra("userInfo", user)
        intent.putExtra("isRoomOwner", isRoomOwner)
        intent.putExtra(WebRTCProperties.EXTRA_SIGNALING_URI, uri)
        intent.putExtra(WebRTCProperties.EXTRA_ROOMID, roomId)
        intent.putExtra(WebRTCProperties.EXTRA_VIDEO_CALL, videoCallEnabled)
        intent.putExtra(WebRTCProperties.EXTRA_SCREENCAPTURE, useScreencapture)
        intent.putExtra(WebRTCProperties.EXTRA_CAMERA2, useCamera2)
        intent.putExtra(WebRTCProperties.EXTRA_VIDEO_WIDTH, videoWidth)
        intent.putExtra(WebRTCProperties.EXTRA_VIDEO_HEIGHT, videoHeight)
        intent.putExtra(WebRTCProperties.EXTRA_VIDEO_FPS, cameraFps)
        intent.putExtra(WebRTCProperties.EXTRA_VIDEO_CAPTUREQUALITYSLIDER_ENABLED,captureQualitySlider)
        intent.putExtra(WebRTCProperties.EXTRA_VIDEO_BITRATE, videoStartBitrate)
        intent.putExtra(WebRTCProperties.EXTRA_VIDEOCODEC, videoCodec)
        intent.putExtra(WebRTCProperties.EXTRA_HWCODEC_ENABLED, hwCodec)
        intent.putExtra(WebRTCProperties.EXTRA_CAPTURETOTEXTURE_ENABLED, captureToTexture)
        intent.putExtra(WebRTCProperties.EXTRA_FLEXFEC_ENABLED, flexfecEnabled)
        intent.putExtra(WebRTCProperties.EXTRA_NOAUDIOPROCESSING_ENABLED, noAudioProcessing)
        intent.putExtra(WebRTCProperties.EXTRA_AECDUMP_ENABLED, aecDump)
        intent.putExtra(WebRTCProperties.EXTRA_SAVE_INPUT_AUDIO_TO_FILE_ENABLED, saveInputAudioToFile)
        intent.putExtra(WebRTCProperties.EXTRA_OPENSLES_ENABLED, useOpenSLES)
        intent.putExtra(WebRTCProperties.EXTRA_DISABLE_BUILT_IN_AEC, disableBuiltInAEC)
        intent.putExtra(WebRTCProperties.EXTRA_DISABLE_BUILT_IN_AGC, disableBuiltInAGC)
        intent.putExtra(WebRTCProperties.EXTRA_DISABLE_BUILT_IN_NS, disableBuiltInNS)
        intent.putExtra(WebRTCProperties.EXTRA_DISABLE_WEBRTC_AGC_AND_HPF, disableWebRtcAGCAndHPF)
        intent.putExtra(WebRTCProperties.EXTRA_AUDIO_BITRATE, audioStartBitrate)
        intent.putExtra(WebRTCProperties.EXTRA_AUDIOCODEC, audioCodec)
        intent.putExtra(WebRTCProperties.EXTRA_DISPLAY_HUD, displayHud)
        intent.putExtra(WebRTCProperties.EXTRA_TRACING, tracing)
        intent.putExtra(WebRTCProperties.EXTRA_ENABLE_RTCEVENTLOG, rtcEventLogEnabled)
        intent.putExtra(WebRTCProperties.EXTRA_DATA_CHANNEL_ENABLED, dataChannelEnabled)
        if (dataChannelEnabled) {
            intent.putExtra(WebRTCProperties.EXTRA_ORDERED, ordered)
            intent.putExtra(WebRTCProperties.EXTRA_MAX_RETRANSMITS_MS, maxRetrMs)
            intent.putExtra(WebRTCProperties.EXTRA_MAX_RETRANSMITS, maxRetr)
            intent.putExtra(WebRTCProperties.EXTRA_PROTOCOL, protocol)
            intent.putExtra(WebRTCProperties.EXTRA_NEGOTIATED, negotiated)
            intent.putExtra(WebRTCProperties.EXTRA_ID, id)
        }

        startForEnterRoomResult.launch(intent)
    }

    private fun startPushMessageService() {
        val pm = applicationContext.getSystemService(POWER_SERVICE) as PowerManager
        var isWhiteListing = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            isWhiteListing = pm.isIgnoringBatteryOptimizations(applicationContext.packageName)
        }
        if (!isWhiteListing) {
            val intent = Intent()
            intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
            intent.data = Uri.parse("package:" + applicationContext.packageName)
            startActivity(intent)
        }

        if (PushMessageService.serviceIntent == null) {
            pushMessageServiceIntent = Intent(this, PushMessageService::class.java)
            startService(pushMessageServiceIntent)
        } else {
            pushMessageServiceIntent = PushMessageService.serviceIntent
            // Toast.makeText(applicationContext, "Already PushMessageService Running", Toast.LENGTH_LONG).show()
        }
    }

    private fun stopPushMessageService() {
        if (pushMessageServiceIntent != null) {
            stopService(pushMessageServiceIntent)
            pushMessageServiceIntent = null
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            // Dynamic permissions are not required before Android M.
            onPermissionsGranted()
            return
        }
        val missingPermissions: Array<String?>? = getMissingPermissions()
        if (missingPermissions?.size != 0) {
            missingPermissions.let {
                requestPermissions(it!!, PERMISSION_REQUEST)
            }
        } else {
            onPermissionsGranted()
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun getMissingPermissions(): Array<String?>? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return arrayOfNulls(0)
        }
        val info: PackageInfo
        info = try {
            packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
        } catch (e: PackageManager.NameNotFoundException) {
            Log.w(TAG, "Failed to retrieve permissions.")
            return arrayOfNulls(0)
        }
        if (info.requestedPermissions == null) {
            Log.w(TAG, "No requested permissions.")
            return arrayOfNulls(0)
        }
        val missingPermissions = ArrayList<String?>()
        for (i in info.requestedPermissions.indices) {
            if (info.requestedPermissionsFlags[i] and PackageInfo.REQUESTED_PERMISSION_GRANTED == 0) {
                missingPermissions.add(info.requestedPermissions[i])
            }
        }
        Log.d(TAG, "Missing permissions: $missingPermissions")
        return missingPermissions.toTypedArray()
    }

    private fun onPermissionsGranted() {
        // If an implicit VIEW intent is launching the app, go directly to that URL.
        val intent = intent
        if ("android.intent.action.VIEW" == intent.action) {
            val loopback = intent.getBooleanExtra(WebRTCProperties.EXTRA_LOOPBACK, false)
            val runTimeMs = intent.getIntExtra(WebRTCProperties.EXTRA_RUNTIME, 0)
            val useValuesFromIntent =
                intent.getBooleanExtra(WebRTCProperties.EXTRA_USE_VALUES_FROM_INTENT, false)
//            val room = sharedPref!!.getString(keyprefRoom, "")
//            connectToRoom(room!!, true, loopback, useValuesFromIntent, runTimeMs)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST) {
            val missingPermissions = getMissingPermissions()
            if (missingPermissions!!.size != 0) {
                // User didn't grant all the permissions. Warn that the application might not work
                // correctly.
                AlertDialog.Builder(this)
                    .setMessage(R.string.missing_permissions_try_again)
                    .setPositiveButton(
                        R.string.yes
                    ) { dialog, id ->
                        // User wants to try giving the permissions again.
                        dialog.cancel()
                        requestPermissions()
                    }
                    .setNegativeButton(
                        R.string.no
                    ) { dialog, id ->
                        // User doesn't want to give the permissions.
                        dialog.cancel()
                        onPermissionsGranted()
                    }
                    .show()
            } else {
                // All permissions granted.
                onPermissionsGranted()
            }
        }
    }

    /**
     * Get a value from the shared preference or from the intent, if it does not
     * exist the default is used.
     */
    private fun sharedPrefGetString(
        attributeId: Int, intentName: String, defaultId: Int
    ): String? {
        val defaultValue = getString(defaultId)
        val attributeName = getString(attributeId)
        return sharedPref!!.getString(attributeName, defaultValue)
    }

    /**
     * Get a value from the shared preference or from the intent, if it does not
     * exist the default is used.
     */
    private fun sharedPrefGetBoolean(
        attributeId: Int, intentName: String, defaultId: Int
    ): Boolean {
        val defaultValue = java.lang.Boolean.parseBoolean(getString(defaultId))
        val attributeName = getString(attributeId)
        return sharedPref!!.getBoolean(attributeName, defaultValue)
    }

    /**
     * Get a value from the shared preference or from the intent, if it does not
     * exist the default is used.
     */
    private fun sharedPrefGetInteger(
        attributeId: Int, intentName: String, defaultId: Int
    ): Int {
        val defaultString = getString(defaultId)
        val defaultValue = defaultString.toInt()
        val attributeName = getString(attributeId)
        val value = sharedPref!!.getString(attributeName, defaultString)
        try {
            return value!!.toInt()
        } catch (e: NumberFormatException) {
            Log.e(TAG, "Wrong setting for: $attributeName:$value")
            return defaultValue
        }
    }
}