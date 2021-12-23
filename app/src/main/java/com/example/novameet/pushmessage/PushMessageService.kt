package com.example.novameet.pushmessage

import android.app.AlarmManager
import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.novameet.MainActivity
import com.example.novameet.R
import com.example.novameet.login.AutoLogin
import com.example.novameet.model.ChatMessage
import com.example.novameet.model.User
import com.example.novameet.room.PushMessageEvents
import com.example.novameet.room.RoomActivity
import com.example.novameet.room.RoomService
import com.example.novameet.room.chat.ChatEvents
import com.example.novameet.room.chat.SocketClient

import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread
import kotlin.concurrent.timer


class PushMessageService : Service() {
    private val TAG = "PushMessageService"

    private var userInfo: User? = null
    // TCP Client
    var tcpClient = SocketClient()
    var isConnected = false
    var isCommunicated = false
    var communicateThread: Thread? = null

    private var receivedLoggedInUsersMessageCallback : ((String?) -> Unit)? = null

    companion object {
        var serviceIntent: Intent? = null
    }

    override fun onCreate() {
        super.onCreate()
    }

    override fun onBind(intent: Intent): IBinder {
        return PushMessageServiceBinder()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: ")
        showToast(application, "Start PushMessageService")

//        userInfo = User(0,
//            "PompitzGeneral@gmail.com" ,
//            "dglee3",
//            "https://novameet.s3.ap-northeast-2.amazonaws.com/1630675219696.jpg",
//            0
//        )
        // shared에서 로그인 유저 정보 가져오기
        userInfo = User(
            AutoLogin.getUserIdx(this),
            AutoLogin.getUserId(this),
            AutoLogin.getUserDisplayName(this),
            AutoLogin.getUserImageUrl(this),
            AutoLogin.getDailyFocusTime(this)
        )

        serviceIntent = intent

        startCommunicate()
        thread {
            connect()
            userInfo?.let {
                sendRequestJoinMessage()
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: ")

        stopCommunicate()
        thread {
            disconnect()
        }

        serviceIntent = null
        setAlarmTimer()
    }

    fun showToast(application: Application, msg: String?) {
        val h = Handler(application.mainLooper)
        h.post { Toast.makeText(application, msg, Toast.LENGTH_LONG).show() }
    }

    fun setReceivedLoggedInUsersMessageCallback( callback : (String?) -> Unit ) {
        Log.d(TAG, "setReceivedLoggedInUsersMessageCallback: ")
        receivedLoggedInUsersMessageCallback = callback
    }

    fun sendRequestJoinMessage() {
        Log.d(TAG, "sendRequestJoinMessage, senderUserId: ${userInfo?.userID}")
        var messageName = "REQUEST_JOIN"
        var message = "${messageName};${userInfo?.userID};${userInfo?.userDisplayName};${userInfo?.userImageUrl}"
        tcpClient.sendData(message)
    }

    fun sendRequestLoggedInUsersMessage() {
        Log.d(TAG, "sendRequestLoggedInUsersMessage")
        var messageName = "REQUEST_CURRENT_USERS"
        var message = "${messageName}"
        tcpClient.sendData(message)
    }

    fun sendPushMessage(receiverUserID: String?, roomID: String?) {
        Log.d(TAG, "sendPushMessage, receiverUserID: ${receiverUserID}")
        var messageName = "REQUEST_PUSH_MESSAGE"
        var message = "${messageName};${receiverUserID};${roomID}"
        tcpClient.sendData(message)
    }

    private fun receivedResponseLoggedInUsersMessage(msg: String?) {
        Log.d(TAG, "receivedResponseLoggedInUsersMessage, msg: ${msg}")
        receivedLoggedInUsersMessageCallback?.invoke(msg)
    }

    private fun receivedPushMessage(msg: String?) {
        Log.d(TAG, "receivedPushMessage: msg: ${msg}")
        var fields = msg?.split(";")
        var senderUserID = fields?.get(1)
        var senderUserDisplayName = fields?.get(2)
        var senderUserImageUrl = fields?.get(3)
        var roomID = fields?.get(4)

        invokeNotification(senderUserID, senderUserDisplayName, senderUserImageUrl, roomID)
    }

    private fun doCommunicate() {
        isCommunicated = true
        while (isCommunicated) {
            // 1. read message
            var message: String? = tcpClient.read()
            message?.let {
                var fields = it.split(";")
                if (fields.get(0) == "RESPONSE_CURRENT_USERS") {
                    receivedResponseLoggedInUsersMessage(message)
                } else if (fields.get(0) == "PUSH_MESSAGE") {
                    receivedPushMessage(message)
                }
            }

            Thread.sleep(100)
        }
    }

    private fun startCommunicate() {
        communicateThread = thread {
            doCommunicate()
        }
    }

    private fun stopCommunicate() {
        isCommunicated = false
        communicateThread = null
    }

    private fun connect() {
//        tcpClient.connect( "192.168.219.103",40001)
        tcpClient.connect( "www.novameet.ga",40001)
    }

    private fun disconnect() {
        tcpClient.closeConnect()
    }

    protected fun setAlarmTimer() {
        Log.d(TAG, "setAlarmTimer: ")
        val c = Calendar.getInstance()
        c.timeInMillis = System.currentTimeMillis()
        c.add(Calendar.SECOND, 1)
        val intent = Intent(this, AlarmReceiver::class.java)
        val sender = PendingIntent.getBroadcast(this, 0, intent, 0)
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        alarmManager[AlarmManager.RTC_WAKEUP, c.timeInMillis] = sender
    }

    private fun invokeNotification(senderUserID: String?,
                                   senderUserDisplayName: String?,
                                   senderUserImageUrl: String?,
                                   roomID: String?) {
        Log.d(TAG, "sendNotification: ")

        val futureTarget = Glide.with(this)
            .asBitmap()
            .load(senderUserImageUrl)
            .apply(RequestOptions()?.circleCrop())
            .submit()

        val largeIconBitmap = futureTarget.get()

        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
//        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        intent.putExtra("isEnteredFromNotification", true);
        intent.putExtra("roomID", roomID)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0 /* Request code */,
            intent,
            PendingIntent.FLAG_CANCEL_CURRENT
            //PendingIntent.FLAG_ONE_SHOT
        )
        var contentsString = "${senderUserDisplayName}님이 ${roomID}방으로 초대하였습니다."
        val channelId = "fcm_default_channel" //getString(R.string.default_notification_channel_id);
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder: NotificationCompat.Builder =
            NotificationCompat.Builder(this, channelId)
                .setContentTitle("NovaMeet 알림")
                .setContentText(contentsString)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(largeIconBitmap)
                .setSound(defaultSoundUri)
                .setPriority(Notification.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
//                .setFullScreenIntent(pendingIntent, true)
                .setAutoCancel(true)
                .setTimeoutAfter(2000)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Channel human readable title",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }
        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build())
    }

    inner class PushMessageServiceBinder : Binder() {
        val service: PushMessageService
            get() = this@PushMessageService
    }
}