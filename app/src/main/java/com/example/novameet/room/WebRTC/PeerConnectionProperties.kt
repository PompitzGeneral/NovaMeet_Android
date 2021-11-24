package com.example.webrtcmultipleandroidsample.WebRTC

object PeerConnectionProperties {
    const val VIDEO_TRACK_ID = "ARDAMSv0"
    const val AUDIO_TRACK_ID = "ARDAMSa0"
    const val VIDEO_TRACK_TYPE = "video"
    const val VIDEO_CODEC_VP8 = "VP8"
    const val VIDEO_CODEC_VP9 = "VP9"
    const val VIDEO_CODEC_H264 = "H264"
    const val VIDEO_CODEC_H264_BASELINE = "H264 Baseline"
    const val VIDEO_CODEC_H264_HIGH = "H264 High"
    const val AUDIO_CODEC_OPUS = "opus"
    const val AUDIO_CODEC_ISAC = "ISAC"
    const val VIDEO_CODEC_PARAM_START_BITRATE = "x-google-start-bitrate"
    const val VIDEO_FLEXFEC_FIELDTRIAL = "WebRTC-FlexFEC-03-Advertised/Enabled/WebRTC-FlexFEC-03/Enabled/"
    const val VIDEO_VP8_INTEL_HW_ENCODER_FIELDTRIAL = "WebRTC-IntelVP8/Enabled/"
    const val DISABLE_WEBRTC_AGC_FIELDTRIAL = "WebRTC-Audio-MinimizeResamplingOnMobile/Enabled/"
    const val AUDIO_CODEC_PARAM_BITRATE = "maxaveragebitrate"
    const val AUDIO_ECHO_CANCELLATION_CONSTRAINT = "googEchoCancellation"
    const val AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT = "googAutoGainControl"
    const val AUDIO_HIGH_PASS_FILTER_CONSTRAINT = "googHighpassFilter"
    const val AUDIO_NOISE_SUPPRESSION_CONSTRAINT = "googNoiseSuppression"
    const val DTLS_SRTP_KEY_AGREEMENT_CONSTRAINT = "DtlsSrtpKeyAgreement"
}