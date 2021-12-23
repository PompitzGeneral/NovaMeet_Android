package com.example.novameet.login

import android.content.Context
import android.content.SharedPreferences

object AutoLogin {
    private const val PREF_USER_IDX = "PREF_USER_IDX"
    private const val PREF_USER_ID = "PREF_USER_ID"
    private const val PREF_USER_DISPLAY_NAME = "PREF_USER_DISPLAY_NAME"
    private const val PREF_USER_IMAGE_URL = "PREF_USER_IMAGE_URL"
    private const val PREF_USER_DAILY_FOCUS_TIME = "PREF_USER_DAILY_FOCUS_TIME"

    // 모든 엑티비티에서 인스턴스를 얻기위함
    fun getSharedPreferences(context: Context): SharedPreferences {
        // return PreferenceManager.getDefaultSharedPreferences(ctx);
        return context.getSharedPreferences(PREF_USER_ID, Context.MODE_PRIVATE)
    }

    fun setUserIdx(context: Context, userIdx: Int) {
        val editor = getSharedPreferences(context).edit()
        editor.putInt(PREF_USER_IDX, userIdx)
        editor.commit()
    }

    fun getUserIdx(context: Context): Int {
        return getSharedPreferences(context).getInt(PREF_USER_IDX, 0)
    }

    fun setUserId(context: Context, userId: String?) {
        val editor = getSharedPreferences(context).edit()
        editor.putString(PREF_USER_ID, userId)
        editor.commit()
    }

    fun getUserId(context: Context): String? {
        return getSharedPreferences(context).getString(PREF_USER_ID, "")
    }

    fun setUserDisplayName(context: Context, userDisplayName: String?) {
        val editor = getSharedPreferences(context).edit()
        editor.putString(PREF_USER_DISPLAY_NAME, userDisplayName)
        editor.commit()
    }

    fun getUserDisplayName(context: Context): String? {
        return getSharedPreferences(context).getString(PREF_USER_DISPLAY_NAME, "")
    }

    fun setUserImageUrl(context: Context, userImageUrl: String?) {
        val editor = getSharedPreferences(context).edit()
        editor.putString(PREF_USER_IMAGE_URL, userImageUrl)
        editor.commit()
    }

    fun getUserImageUrl(context: Context): String? {
        return getSharedPreferences(context).getString(PREF_USER_IMAGE_URL, "")
    }

    fun setDailyFocusTime(context: Context, dailyFocusTime: Int) {
        val editor = getSharedPreferences(context).edit()
        editor.putInt(PREF_USER_DAILY_FOCUS_TIME, dailyFocusTime)
        editor.commit()
    }

    fun getDailyFocusTime(context: Context): Int {
        return getSharedPreferences(context).getInt(PREF_USER_DAILY_FOCUS_TIME, 0)
    }

    // 로그아웃 : 자동 로그인 해제 및 로그아웃 시 호출 될 메소드
    fun clearUserInfo(context: Context) {
        val editor = getSharedPreferences(context).edit()
        editor.clear()
        editor.commit()
    }
}
