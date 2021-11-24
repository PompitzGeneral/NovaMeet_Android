package com.example.novameet.room.WebRTC;

import com.example.novameet.model.User;

/**
 * Struct holding the connection parameters of an AppRTC room.
 */
public class RoomConnectionParameters {
    public final String signalingServerUrl;
    public final User userInfo;
    public final String roomId;
    public final boolean isRoomOwner;
    public final boolean loopback;
    public RoomConnectionParameters(
            String signalingServerUrl, User userInfo, String roomId, boolean isRoomOwner, boolean loopback) {
        this.signalingServerUrl = signalingServerUrl;
        this.userInfo = userInfo;
        this.roomId = roomId;
        this.isRoomOwner = isRoomOwner;
        this.loopback = loopback;
    }
}
