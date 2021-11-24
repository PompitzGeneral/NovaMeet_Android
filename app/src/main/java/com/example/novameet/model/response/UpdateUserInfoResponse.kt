package com.example.novameet.model.response

data class UpdateUserInfoResponse(
    val responseCode: Int,
    val user_id: String?,
    val user_displayname: String?,
    val user_image: String?
    )
