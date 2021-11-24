package com.example.novameet.member

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.novameet.databinding.ActivityMemberBinding

class MemberActivity : AppCompatActivity() {
    private val TAG : String = "MemberActivity"
    private val binding: ActivityMemberBinding by lazy { ActivityMemberBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
    }
}