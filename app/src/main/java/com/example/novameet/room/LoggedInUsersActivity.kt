package com.example.novameet.room

import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.novameet.R
import com.example.novameet.databinding.ActivityLoggedInUsersBinding
import com.example.novameet.model.ChatMessage
import com.example.novameet.model.LoggedInUsersRVItem
import com.example.novameet.model.Room
import com.example.novameet.pushmessage.PushMessageService

class LoggedInUsersActivity : AppCompatActivity() {
    private val TAG : String = "LoggedInUsersActivity"

    private val binding: ActivityLoggedInUsersBinding by lazy { ActivityLoggedInUsersBinding.inflate(layoutInflater) }

    private var loggedInUserID: String? = null
    private var roomID: String? = null
    private val pushMessageButtonClickCallback: ( (userID: String?) -> Unit ) = { userID ->
        Log.d(TAG, "pushMessageButtonClickCallback invoked")
        requestPushMessage(userID, roomID)
    }

    private val receivedLoggedInUsersMessageCallback: (msg: String?) -> Unit = { msg ->
        Log.d(TAG, "onReceivedLoggedInUsersMessageCallback invoked")
        receivedLoggedInUsersMessage(msg)
    }

    private var pushMessageServiceBinder: PushMessageService.PushMessageServiceBinder? = null
    private val pushMessageServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            Log.d(TAG, "onServiceConnected: ")

            val myToast = Toast.makeText(applicationContext,
                "LoggedInUsersActivity - onServiceConnected",
                Toast.LENGTH_SHORT)
            myToast.show()

            pushMessageServiceBinder = binder as PushMessageService.PushMessageServiceBinder

            pushMessageServiceBinder?.service?.setReceivedLoggedInUsersMessageCallback(
                receivedLoggedInUsersMessageCallback
            )

            reqeustLoggedInUsers()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "onServiceDisconnected: ")
            val myToast = Toast.makeText(applicationContext,
                "LoggedInUsersActivity - onServiceDisconnected",
                Toast.LENGTH_SHORT)
            myToast.show()

            pushMessageServiceBinder = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        loggedInUserID = intent.getStringExtra("loginUserID")
        roomID = intent.getStringExtra("roomID")

        initRecyclerView()

        bindToPushMessageService()
    }

    override fun onDestroy() {
        super.onDestroy()
        unBindToPushMessageService()
    }

    private fun initRecyclerView() {
        Log.d(TAG, "initRecyclerView: ")
        binding?.recyclerView?.adapter = LoggedInUsersRVAdapter(pushMessageButtonClickCallback)
        binding?.recyclerView?.layoutManager = LinearLayoutManager(this)
    }

    private fun bindToPushMessageService() {
        Log.d(TAG, "bindToPushMessageService: ")
        Intent(this, PushMessageService::class.java).run {
            bindService(this, pushMessageServiceConnection, Service.BIND_AUTO_CREATE)
        }
    }

    private fun unBindToPushMessageService() {
        Log.d(TAG, "unBindToPushMessageService: ")
        unbindService(pushMessageServiceConnection)
    }

    private fun reqeustLoggedInUsers() {
        Log.d(TAG, "reqeustLoggedInUsers: ")
        pushMessageServiceBinder?.service?.sendRequestLoggedInUsersMessage()
    }

    private fun requestPushMessage(userID: String?, roomID: String?) {
        Log.d(TAG, "requestPushMessage: ")
        pushMessageServiceBinder?.service?.sendPushMessage(userID, roomID)
    }

    private fun receivedLoggedInUsersMessage(msg: String?) {
        Log.d(TAG, "onReceivedLoggedInUsersMessage: ")

        var fields = msg?.split(";")
        var userCount: Int = fields?.get(1)?.toInt() ?: 0

        var users = arrayListOf<LoggedInUsersRVItem>()
        if (userCount >= 1) {
            // Todo. Refactoring
            for (i in 0 until userCount) {
                var userInfoPart = fields?.get(i + 2)
                var userInfos = userInfoPart?.split("*")

                loggedInUserID?.let {
                    if ( !it?.equals( userInfos?.get(0) ) ) {
                        users.add(
                            LoggedInUsersRVItem (
                                userInfos?.get(0),
                                userInfos?.get(1),
                                userInfos?.get(2)
                            )
                        )
                    }
                }
            }
        }

        runOnUiThread {
            var adapter = binding?.recyclerView?.adapter as LoggedInUsersRVAdapter
            adapter?.setItems(users)
        }
    }
}