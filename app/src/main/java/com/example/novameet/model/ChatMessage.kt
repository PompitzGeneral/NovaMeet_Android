package com.example.novameet.model

data class ChatMessage(
    var message: String?,
    var sender: User?,
    var createdAt: Long,
)

