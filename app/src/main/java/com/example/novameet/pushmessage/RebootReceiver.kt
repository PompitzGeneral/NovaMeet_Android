package com.example.novameet.pushmessage

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class RebootReceiver : BroadcastReceiver() {
    private val TAG = "RebootReceiver"
    override fun onReceive(context: Context, intent: Intent) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d(TAG, "onReceive: Build.VERSION.SDK_INT >= Build.VERSION_CODES.O")
            val intent = Intent(context, RestartService::class.java)
            context.startForegroundService(intent)
        } else {
            Log.d(TAG, "onReceive: Build.VERSION.SDK_INT < Build.VERSION_CODES.O")
            val intent = Intent(context, PushMessageService::class.java)
            context.startService(intent)
        }
    }
}