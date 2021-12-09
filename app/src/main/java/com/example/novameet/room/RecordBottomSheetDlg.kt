package com.example.novameet.room

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.novameet.databinding.FragmentRecordBottomSheetDlgBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class RecordBottomSheetDlg(val event: RecordBottomSheetDlgEvent) : BottomSheetDialogFragment() {

    private var binding: FragmentRecordBottomSheetDlgBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentRecordBottomSheetDlgBinding.inflate(inflater, container, false)
        val view = binding?.root

        // 실질적인 로직은 RoomActivity에서 관리
        binding?.recordStartButton?.setOnClickListener {
            event.onRecordStartButtonClicked()
        }

        binding?.recordStopButton?.setOnClickListener {
            event.onRecordStopButtonClicked()
        }

        event.onDlgCreateView()

        return view
    }

    fun updateFocusTimeView(focusTime: Int) {
        var hour = focusTime / 60 / 60
        var min  = (focusTime / 60) % 60
        var sec  = focusTime % 60

        binding?.txtviewFocusTime?.text =
            String.format("%02d", hour) + ":" +
            String.format("%02d", min) + ":" +
            String.format("%02d", sec)
    }

    companion object {
        @JvmStatic
        fun newInstance(event: RecordBottomSheetDlgEvent): RecordBottomSheetDlg {
            return RecordBottomSheetDlg(event).apply {
            }
        }
    }
}