package com.example.novameet.model.response

data class LoginResponse(
    val responseCode: Int,
    val user_id: String?,
    val user_displayname: String?,
    val user_image_url: String?
)
