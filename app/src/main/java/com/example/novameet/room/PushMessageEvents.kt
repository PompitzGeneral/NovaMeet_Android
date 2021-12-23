package com.example.novameet.room

interface PushMessageEvents {
    fun onReceivedLoggedInUsersMessage(msg: String?)
}