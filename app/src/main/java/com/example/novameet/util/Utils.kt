package com.example.novameet.util

import android.util.Log
import java.text.SimpleDateFormat

class Utils {
    companion object {
        val TAG = "Utils"

        fun convertDateToTimestamp(date: String): Long {
            val sdf = SimpleDateFormat("yyyy-MM-dd")
            Log.d("TTTT Time -> ", sdf.parse(date).time.toString())
            Log.d("TTT Unix -> ", (System.currentTimeMillis()).toString())
            return sdf.parse(date).time
        }

        fun convertTimestampToDate(timestamp: Long): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd-hh-mm")
            val date = sdf.format(timestamp)
            Log.d("TTT UNix Date -> ", sdf.format((System.currentTimeMillis())).toString())
            Log.d("TTTT date -> ", date)

            return date
        }

        fun convertTimestampToHourAndMinute(timestamp: Long): String {
            val sdf1 = SimpleDateFormat("hh:mm")
            var date = sdf1.format(timestamp)
            Log.d("TTT UNix Date -> ", sdf1.format((System.currentTimeMillis())).toString())
            Log.d("TTTT date -> ", date)

            val sdf2 = SimpleDateFormat("aa")
            val ampm = sdf2.format(timestamp)

            if (ampm == "PM") {
                date = "오후 " + date
            } else if (ampm == "AM") {
                date = "오전 " + date
            } else {
                date = ampm + date
            }

            return date
        }
    }
}