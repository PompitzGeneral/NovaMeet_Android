package com.example.novameet.chat

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.novameet.databinding.ActivityChatBinding

class ChatActivity : AppCompatActivity() {
    private val TAG : String = "ChatActivity"
    private val binding: ActivityChatBinding by lazy { ActivityChatBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
    }
}