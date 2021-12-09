package com.example.novameet.network

import com.google.gson.JsonElement
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*

interface IRetrofit {
    @FormUrlEncoded
    @POST("api/login")
    fun requestLogin(
        @Field("user_id") userId: String?,
        @Field("user_pw") password: String?
    ): Call<JsonElement>

    @POST("api/logout")
    fun requestLogout(

    ): Call<JsonElement>

    @FormUrlEncoded
    @POST("api/emailAuth")
    fun requestEmailAuth(
        @Field("user_email") userEmail: String?,
        @Field("auth_number") authNumber: String?
    ): Call<JsonElement>

    @FormUrlEncoded
    @POST("api/register")
    fun requestRegister(
        @Field("user_id") userEmail: String?,
        @Field("user_pw") userPassword: String?,
        @Field("user_displayname") displayName: String?
    ): Call<JsonElement>

    @Multipart
    @POST("api/updateUserInfo")
    fun requestUpdateUserInfo(
        @Part file: MultipartBody.Part?,
        @Part("user_id") userEmail: RequestBody?,
        @Part("user_displayname") userDisplayName: RequestBody?
    ): Call<JsonElement>

    @FormUrlEncoded
    @POST("api/updateUserPassword")
    fun requestUpdateUserPassword(
        @Field("user_id") userEmail: String?,
        @Field("user_password") userCurrentPassword: String?,
        @Field("user_new_password") userNewPassword: String?
    ): Call<JsonElement>

    @POST("api/requestRoomInfos")
    fun requestRoomInfos(

    ): Call<JsonElement>

    @FormUrlEncoded
    @POST("api/joinRoom")
    fun requestJoinRoom(
        @Field("userID") userId: String?,
        @Field("roomID") roomId: String?,
        @Field("inputPassword") inputPassword: String?
    ): Call<JsonElement>

    @Multipart
    @POST("api/createRoom")
    fun requestCreateRoom(
        @Part("roomID") roomID: RequestBody?,
        @Part("roomOwner") roomOwner: RequestBody?,
        @Part roomThumbnail: MultipartBody.Part?,
        @Part("roomPassword") roomPassword: RequestBody?,
        @Part("roomMemberMaxCount") roomMemberMaxCount: RequestBody?,
    ): Call<JsonElement>

    @FormUrlEncoded
    @POST("api/deleteRoom")
    fun requestDeleteRoom(
        @Field("roomID") roomId: String?,
    ): Call<JsonElement>

    @FormUrlEncoded
    @POST("api/updateDailyFocusTime")
    fun requestUpdateDailyFocusTime(
        @Field("userIdx") userIdx: Int,
        @Field("dailyFocusTime") dailyFocusTime: Int,
    ): Call<JsonElement>
}