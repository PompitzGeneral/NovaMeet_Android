package com.example.novameet.room.chat

import org.webrtc.IceCandidate

/**
 * TCP Chat Events.
 */
interface ChatEvents {
    /**
     * Callback fired once Received Message
     */
    fun onReceivedMessage()

    /**
     * Callback fired once TCP Connected
     */
    fun onConnected()

    /**
     * Callback fired once TCP Disconnected
     */
    fun onDisconnected()
}