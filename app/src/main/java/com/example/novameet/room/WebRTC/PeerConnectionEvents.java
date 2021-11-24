package com.example.novameet.room.WebRTC;

import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;
import org.webrtc.StatsReport;
import org.webrtc.VideoTrack;

/**
 * Peer connection events.
 */
public interface PeerConnectionEvents {
    /**
     * Callback fired once local SDP is created and set.
     */
    void onLocalDescription(final String remoteSocketId, final SessionDescription sdp);

    /**
     * Callback fired once local Ice candidate is generated.
     */
    void onIceCandidate(final String remoteSocketId, final IceCandidate candidate);

    /**
     * Callback fired once local ICE candidates are removed.
     */
    void onIceCandidatesRemoved(final IceCandidate[] candidates);

    /**
     * Callback fired once connection is established (IceConnectionState is
     * CONNECTED).
     */
    void onIceConnected();

    /**
     * Callback fired once connection is disconnected (IceConnectionState is
     * DISCONNECTED).
     */
    void onIceDisconnected();

    /**
     * Callback fired once DTLS connection is established (PeerConnectionState
     * is CONNECTED).
     */
    void onConnected(final String remoteSocketId);

    /**
     * Callback fired once DTLS connection is disconnected (PeerConnectionState
     * is DISCONNECTED).
     */
    void onDisconnected();

    /**
     * Callback fired once peer connection is closed.
     */
    void onPeerConnectionClosed();

    /**
     * Callback fired once peer connection statistics is ready.
     */
    void onPeerConnectionStatsReady(final StatsReport[] reports);

    /**
     * Callback fired once peer connection error happened.
     */
    void onPeerConnectionError(final String description);

    /**
     * Callback fired once peer connection Add Stream.
     */
    void onAddStream(final String remoteSocketId, final VideoTrack videoTrack);
}