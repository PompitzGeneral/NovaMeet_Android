package com.example.novameet.model

import java.io.Serializable

data class User (
    var userIdx: Int,
    var userID: String?,
    var userDisplayName: String?,
    var userImageUrl: String?,
    var dailyFocusTime: Int
) : Serializable
