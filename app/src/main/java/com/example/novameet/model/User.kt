package com.example.novameet.model

import java.io.Serializable

data class User (
    var userID: String?,
    var userDisplayName: String?,
    var userImageUrl: String?,
) : Serializable
