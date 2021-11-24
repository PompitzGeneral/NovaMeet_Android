package com.example.novameet.model

data class Room(
    var roomId: String?,
    var hasPassword: Boolean,
    var roomOwner: String?,
    var roomOwnerImageUrl: String?,
    var roomMemberCurrentCount: Int?,
    var roomMemberMaxCount: Int?,
    var roomThumbnailUrl: String?,
)
