package com.example.novameet.network
import androidx.preference.PreferenceManager

import android.content.Context
import android.util.Log
import com.example.novameet.model.Room
import com.example.novameet.model.response.*
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import okhttp3.MultipartBody
import okhttp3.RequestBody


class RetrofitManager() {
    private val BASE_URL = "https://www.novameet.ga"
    private val TAG: String = "[RetrofitManager]"

    private var appContext: Context? = null;

    companion object{
        val instance = RetrofitManager()
    }

    private val iRetrofit: IRetrofit? = RetrofitClient.getClient(BASE_URL, appContext)?.create(IRetrofit::class.java)

    fun setContext(context: Context) {
        appContext = context
    }
    fun requestLogin(userEmail: String?, password: String?, completion: (LoginResponse) -> Unit) {
        var call = iRetrofit?.requestLogin(userEmail, password) ?: return

        call.enqueue(object: Callback<JsonElement>{
            override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                Log.d(TAG, "login - onFailure: ")
            }

            override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                Log.d(TAG, "login - onResponse: ${response.body()} ")
                Log.d(TAG, "login - onResponse: status code is ${response.code()}")
                Log.d(TAG, "login - onResponse: header is : ${response.headers()} ")

                val cookielist = response.headers().values("Set-Cookie")
                if (cookielist.size > 0) {
                    var sessionCookie = cookielist.get(0)
                    android.webkit.CookieManager.getInstance()?.setCookie("https://www.novameet.ga", sessionCookie)
                }

                val loginResponse = Gson().fromJson<LoginResponse>(response.body(), LoginResponse::class.java)
                if(response.code() == 200) {
                    completion(loginResponse)
                } else {
                    // badgate -> 504
                    // completion(loginResponse)
                }
            }
        })
    }

    fun requestLogout(completion: (LogoutResponse) -> Unit) {
        var call = iRetrofit?.requestLogout() ?: return
        call.enqueue(object: Callback<JsonElement>{
            override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                Log.d(TAG, "logout - onFailure: ")
            }

            override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                Log.d(TAG, "logout - onResponse: ${response.body()} ")
                Log.d(TAG, "logout - onResponse: status code is ${response.code()}")

                val logoutResponse = Gson().fromJson<LogoutResponse>(response.body(), LogoutResponse::class.java)
                if(response.code() == 200) {
                    completion(logoutResponse)
                } else {
                    // badgate -> 504
                    // completion(loginResponse)
                }
            }
        })
    }

    fun requestEmailAuth(userEmail: String?, authNumber: String?, completion: (EmailAuthResponse) -> Unit) {
        var call = iRetrofit?.requestEmailAuth(userEmail, authNumber) ?: return
        call.enqueue(object: Callback<JsonElement>{
            override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                Log.d(TAG, "emailAuth - onFailure: ")
            }

            override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                Log.d(TAG, "emailAuth - onResponse: ${response.body()} ")
                Log.d(TAG, "emailAuth - onResponse: status code is ${response.code()}")

                val emailAuthResponse = Gson().fromJson<EmailAuthResponse>(response.body(), EmailAuthResponse::class.java)
                if(response.code() == 200) {
                    completion(emailAuthResponse)
                } else {
                    // badgate -> 504
                    // completion(loginResponse)
                }
            }
        })
    }

    fun requestRegister(userEmail: String?, password: String?, displayName: String?, completion: (RegisterResponse) -> Unit) {
        var call = iRetrofit?.requestRegister(userEmail, password, displayName) ?: return
        call.enqueue(object: Callback<JsonElement>{
            override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                Log.d(TAG, "emailAuth - onFailure: ")
            }

            override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                Log.d(TAG, "emailAuth - onResponse: ${response.body()} ")
                Log.d(TAG, "emailAuth - onResponse: status code is ${response.code()}")

                val registerResponse = Gson().fromJson<RegisterResponse>(response.body(), RegisterResponse::class.java)
                if (response.code() == 200) {
                    completion(registerResponse)
                } else {
                    // badgate -> 504
                    // completion(loginResponse)
                }
            }
        })
    }

    fun requestUpdateUserInfo(filePath: String?,
                              userEmail: String?,
                                  userDisplayName: String?,
                                  completion: (UpdateUserInfoResponse) -> Unit) {

        val file = File(filePath)

        val requestFile: RequestBody = RequestBody.create("image/*".toMediaTypeOrNull(), file)
        val userEmailBody: RequestBody = RequestBody.create("text/plain".toMediaTypeOrNull(), userEmail!!)
        val userDisplayNameBody: RequestBody = RequestBody.create("text/plain".toMediaTypeOrNull(), userDisplayName!!)

        var fileImage: MultipartBody.Part? = MultipartBody.Part.createFormData("file", file.name, requestFile)

        try {
            var call = iRetrofit?.requestUpdateUserInfo(fileImage, userEmailBody, userDisplayNameBody) ?: return

            call.enqueue(object: Callback<JsonElement>{
                override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                    Log.d(TAG, "updateUserPassword - onFailure: ")
                }

                override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                    Log.d(TAG, "updateUserPassword - onResponse: ${response.body()} ")
                    Log.d(TAG, "updateUserPassword - onResponse: status code is ${response.code()}")

                    val updateUserInfoResponse =
                        Gson().fromJson<UpdateUserInfoResponse>(response.body(), UpdateUserInfoResponse::class.java)
                    if (response.code() == 200) {
                        completion(updateUserInfoResponse)
                    } else {
                        // badgate -> 504
                        // completion(loginResponse)
                    }
                }
            })
        }catch(e: Exception){
            Log.d(TAG, "requestUpdateUserInfo: ${e}")
        }

    }

    fun requestUpdateUserPassword(userEmail: String?,
                                          currentPassword: String?,
                                          newPassword: String?,
                                          completion: (UpdateUserPasswordResponse) -> Unit) {
        var call = iRetrofit?.requestUpdateUserPassword(userEmail, currentPassword, newPassword) ?: return
        call.enqueue(object: Callback<JsonElement>{
            override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                Log.d(TAG, "updateUserPassword - onFailure: ")
            }

            override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                Log.d(TAG, "updateUserPassword - onResponse: ${response.body()} ")
                Log.d(TAG, "updateUserPassword - onResponse: status code is ${response.code()}")

                val updateUserPasswordResponse =
                    Gson().fromJson<UpdateUserPasswordResponse>(response.body(), UpdateUserPasswordResponse::class.java)
                if (response.code() == 200) {
                    completion(updateUserPasswordResponse)
                } else {
                    // badgate -> 504
                    // completion(loginResponse)
                }
            }
        })
    }

    fun requestRoomInfos(completion: (RoomInfosResponse) -> Unit) {
        try {
            var call = iRetrofit?.requestRoomInfos() ?: return
            call.enqueue(object: Callback<JsonElement>{
                override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                    Log.d(TAG, "requestRoomInfos - onFailure: ")
                }

                override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                    Log.d(TAG, "requestRoomInfos - onResponse: ${response.body()} ")
                    Log.d(TAG, "requestRoomInfos - onResponse: header is : ${response.headers()} ")
                    Log.d(TAG, "requestRoomInfos - onResponse: status code is ${response.code()}")
                    val cookielist = response.headers().values("Set-Cookie")
                    if (cookielist.size > 0) {
                        var sessionCookie = cookielist.get(0)
                        android.webkit.CookieManager.getInstance()?.setCookie("https://www.novameet.ga", sessionCookie)
                    }

                    var jObject = response.body()?.asJsonObject
                    var jArray = jObject?.get("roomInfos")?.asJsonArray

                    var rooms: ArrayList<Room> = arrayListOf()

                    if (jArray != null) {
                        val count = jArray?.size()!!
                        for (i in 0 until count ) {

                            var roomId                 = jArray?.get(i)?.asJsonObject?.get("roomID")?.asString?:null
                            var hasPassword            = jArray?.get(i)?.asJsonObject?.get("hasPassword")?.asBoolean?: false
                            var roomOwner              = jArray?.get(i)?.asJsonObject?.get("roomOwner")?.asString?:null
                            var roomOwnerImageUrl      = jArray?.get(i)?.asJsonObject?.get("roomOwnerImageUrl")?.asString?:null
                            var roomMemberCurrentCount = jArray?.get(i)?.asJsonObject?.get("roomMemberCurrentCount")?.asInt?:null
                            var roomMemberMaxCount     = jArray?.get(i)?.asJsonObject?.get("roomMemberMaxCount")?.asInt?:null

                            var roomThumbnailUrl: String? = "https://novameet.s3.ap-northeast-2.amazonaws.com/novameet_amazon.jpg"
                            if (!jArray?.get(i)?.asJsonObject?.get("roomThumbnailUrl")?.isJsonNull!!) {
                                roomThumbnailUrl = jArray?.get(i)?.asJsonObject?.get("roomThumbnailUrl")?.asString
                            }

                            rooms.add(
                                Room(roomId,
                                    hasPassword,
                                    roomOwner,
                                    roomOwnerImageUrl,
                                    roomMemberCurrentCount,
                                    roomMemberMaxCount,
                                    roomThumbnailUrl
                                ));
                        }

                        val roomInfosResponse = RoomInfosResponse(rooms);
                        if (response.code() == 200) {
                            completion(roomInfosResponse)
                        } else {
                            // badgate -> 504
                            // completion(loginResponse)
                        }
                    }
                }
            })
        }catch(e: Exception){
            e.printStackTrace()
            Log.d(TAG, e.toString())
        }
    }

    fun requestJoinRoom(userId: String?,
                        roomId: String?,
                        inputPassword: String?,
                        completion: (JoinRoomResponse) -> Unit) {
        var call = iRetrofit?.requestJoinRoom(userId, roomId, inputPassword) ?: return
        call.enqueue(object: Callback<JsonElement>{
            override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                Log.d(TAG, "requestJoinRoom - onFailure: ")
            }

            override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                Log.d(TAG, "requestJoinRoom - onResponse: ${response.body()} ")
                Log.d(TAG, "requestJoinRoom - onResponse: status code is ${response.code()}")

                val joinRoomResponse =
                    Gson().fromJson<JoinRoomResponse>(response.body(), JoinRoomResponse::class.java)
                if (response.code() == 200) {
                    completion(joinRoomResponse)
                } else {
                    // badgate -> 504
                    // completion(loginResponse)
                }
            }
        })
    }

    fun requestCreateRoom(roomId: String?,
                          roomOwner: String?,
                          roomThumbnailPath: String?,
                          roomPassword: String?,
                          roomMemberMaxCount: String?,
                          completion: (CreateRoomResponse) -> Unit) {

        var fileImage: MultipartBody.Part? = null
        if (roomThumbnailPath != null) {
            var roomThumbnailFile: File = File(roomThumbnailPath)
            val requestFile: RequestBody = RequestBody.create("image/*".toMediaTypeOrNull(), roomThumbnailFile)
//            fileImage = MultipartBody.Part.createFormData("file", roomThumbnailFile?.name, requestFile)
            fileImage = MultipartBody.Part.createFormData("roomThumbnail", roomThumbnailFile?.name, requestFile)

        }

        val roomIdBody: RequestBody? = RequestBody.create("text/plain".toMediaTypeOrNull(), roomId!!)
        val roomOwnerBody: RequestBody? = RequestBody.create("text/plain".toMediaTypeOrNull(), roomOwner!!)
        val roomPasswordBody: RequestBody? = RequestBody.create("text/plain".toMediaTypeOrNull(), roomPassword!!)
        val roomMemberMaxCountBody: RequestBody? = RequestBody.create("text/plain".toMediaTypeOrNull(), roomMemberMaxCount.toString()!!)

        var call = iRetrofit?.requestCreateRoom(roomIdBody, roomOwnerBody, fileImage, roomPasswordBody, roomMemberMaxCountBody) ?: return
        call.enqueue(object: Callback<JsonElement>{
            override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                Log.d(TAG, "requestJoinRoom - onFailure: ")
            }

            override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                Log.d(TAG, "requestCreateRoom - onResponse: ${response.body()} ")
                Log.d(TAG, "requestCreateRoom - onResponse: status code is ${response.code()}")

                val createRoomResponse =
                    Gson().fromJson<CreateRoomResponse>(response.body(), CreateRoomResponse::class.java)
                if (response.code() == 200) {
                    completion(createRoomResponse)
                } else {
                    // badgate -> 504
                    // completion(loginResponse)
                }
            }
        })
    }

    fun requestDeleteRoom(roomId: String?, completion: (DeleteRoomResponse) -> Unit) {
        var call = iRetrofit?.requestDeleteRoom(roomId) ?: return
        call.enqueue(object: Callback<JsonElement>{
            override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                Log.d(TAG, "requestDeleteRoom - onFailure: ")
            }

            override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                Log.d(TAG, "requestDeleteRoom - onResponse: ${response.body()} ")
                Log.d(TAG, "requestDeleteRoom - onResponse: status code is ${response.code()}")

                val deleteRoomResponse =
                    Gson().fromJson<DeleteRoomResponse>(response.body(), DeleteRoomResponse::class.java)
                if (response.code() == 200) {
                    completion(deleteRoomResponse)
                } else {
                    // badgate -> 504
                    // completion(loginResponse)
                }
            }
        })
    }

    fun requestUpdateDailyFocusTime(userIdx: Int, dailyFocusTime: Int, completion: (UpdateDailyFocusTimeResponse) -> Unit) {
        var call = iRetrofit?.requestUpdateDailyFocusTime(userIdx, dailyFocusTime) ?: return
        call.enqueue(object: Callback<JsonElement>{
            override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                Log.d(TAG, "requestUpdateDailyFocusTime - onFailure: ")
            }

            override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                Log.d(TAG, "requestUpdateDailyFocusTime - onResponse: ${response.body()} ")
                Log.d(TAG, "requestUpdateDailyFocusTime - onResponse: status code is ${response.code()}")

                val updateDailyFocusTimeResponse =
                    Gson().fromJson<UpdateDailyFocusTimeResponse>(response.body(), UpdateDailyFocusTimeResponse::class.java)
                if (response.code() == 200) {
                    completion(updateDailyFocusTimeResponse)
                } else {
                    // badgate -> 504
                    // completion(loginResponse)
                }
            }
        })
    }
}