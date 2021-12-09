package com.example.novameet.room

interface RecordBottomSheetDlgEvent {
    fun onDlgCreateView()
    fun onRecordStartButtonClicked()
    fun onRecordStopButtonClicked()
    fun onUpdatedFocusTime(focusTime: Int)
}