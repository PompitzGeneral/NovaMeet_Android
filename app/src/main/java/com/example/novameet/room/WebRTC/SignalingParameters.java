package com.example.novameet.room.WebRTC;

import org.webrtc.IceCandidate;
import org.webrtc.PeerConnection;

import java.util.List;

public class SignalingParameters {
    public final List<PeerConnection.IceServer> iceServers;
    public final boolean initiator;
    public final List<IceCandidate> iceCandidates;

    public SignalingParameters(List<PeerConnection.IceServer> iceServers, boolean initiator,
                               List<IceCandidate> iceCandidates) {
        this.iceServers = iceServers;
        this.initiator = initiator;
        this.iceCandidates = iceCandidates;
    }
}
