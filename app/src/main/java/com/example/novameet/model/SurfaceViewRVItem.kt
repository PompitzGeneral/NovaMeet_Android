package com.example.novameet.model

import org.webrtc.VideoTrack

data class SurfaceViewRVItem(
    var socketID : String?,
    var userID: String?,
    var userDisplayName: String?,
    var userImageUrl: String?,
    var isVideoEnabled: Boolean,
    var isAudioEnabled: Boolean,
    var track: VideoTrack?
)
