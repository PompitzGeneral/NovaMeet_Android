package com.example.novameet.room.chat

import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.novameet.databinding.ActivityChatBinding
import com.example.novameet.model.ChatMessage
import com.example.novameet.room.RoomService

class ChatActivity : AppCompatActivity(), ChatEvents {
    private val TAG : String = "ChatActivity"
    private val binding: ActivityChatBinding by lazy { ActivityChatBinding.inflate(layoutInflater) }

    private val messageList: ArrayList<ChatMessage> = arrayListOf()
    private var roomServiceBinder: RoomService.RoomServiceBinder? = null
    private val roomServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            roomServiceBinder = service as RoomService.RoomServiceBinder

            val myToast = Toast.makeText(applicationContext,
                "ChatActivity - onServiceConnected",
                Toast.LENGTH_SHORT)
            myToast.show()

            // 현재 메시지 목록 세팅
            var chatManager = roomServiceBinder?.service?.chatManager

            var adapter = binding?.rvChat?.adapter as MessageListAdapter
            adapter?.setItems(chatManager!!.messageList);
            if (adapter.itemCount >= 1) {
                binding?.rvChat?.smoothScrollToPosition(adapter.itemCount-1)
            }

            chatManager?.events = this@ChatActivity
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            roomServiceBinder = null

            val myToast = Toast.makeText(applicationContext,
                "BinderExtendedService - onServiceDisconnected",
                Toast.LENGTH_SHORT)
            myToast.show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        var loginUserID = intent.getStringExtra("loginUserID")
        binding?.rvChat?.adapter = MessageListAdapter(
            this,
            messageList,
            loginUserID
        )
        binding.rvChat.layoutManager = LinearLayoutManager(this)
        binding.buttonChatSend.setOnClickListener {
            sendMessage()
        }

        bindToRoomService()
    }

    override fun onDestroy() {
        unBindFromRoomService()
        super.onDestroy()
    }

    private fun bindToRoomService() {
        Intent(this, RoomService::class.java).run {
            bindService(this, roomServiceConnection, Service.BIND_AUTO_CREATE)
        }
    }

    private fun unBindFromRoomService() {
        unbindService(roomServiceConnection)
    }

    private fun sendMessage() {
        var chatManager = roomServiceBinder?.service?.chatManager
        chatManager?.sendChatMessage(binding.editChatMessage.text.toString())
        binding.editChatMessage.text.clear()
    }

    /*
     * TCP Chat Events
     */
    override fun onReceivedMessage() {
        runOnUiThread {

            var adapter = binding?.rvChat?.adapter as MessageListAdapter
            if (adapter.itemCount >= 1) {
                adapter?.notifyItemInserted(adapter?.itemCount - 1)
                binding?.rvChat?.smoothScrollToPosition(adapter?.itemCount - 1)
//                adapter?.notifyDataSetChanged()
            }
        }
    }

    override fun onConnected() {
        TODO("Not yet implemented")
    }

    override fun onDisconnected() {
        TODO("Not yet implemented")
    }
}