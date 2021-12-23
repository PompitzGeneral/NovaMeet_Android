package com.example.novameet.network
import android.content.Context
import android.provider.SyncStateContract
import androidx.preference.PreferenceManager

import android.util.Log
import okhttp3.CookieJar
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import okhttp3.Cookie
import okhttp3.HttpUrl
import java.net.CookieManager
import java.util.*


object RetrofitClient {
    private var retrofitClient: Retrofit? = null
    private val TAG: String = "[RetrofitClient]"

    fun getClient(baseUrl: String, context: Context?): Retrofit?{
        if (retrofitClient == null) {
            val client = OkHttpClient
                .Builder()
                .cookieJar(JavaNetCookieJar(CookieManager())) // 세션정보 쿠키저장 하기 위함

            val loggingInterceptor = HttpLoggingInterceptor(object: HttpLoggingInterceptor.Logger {
                override fun log(message: String) {
                    Log.d(TAG, "RetrofitClient - log: $message");
                }
            })

            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY)
            client.addInterceptor(loggingInterceptor)
            client.connectTimeout(10, TimeUnit.SECONDS)
            client.readTimeout(10, TimeUnit.SECONDS)
            client.writeTimeout(10, TimeUnit.SECONDS)
            client.retryOnConnectionFailure(true)

            try {
                retrofitClient = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client.build())
                    .build()
            } catch (e: Exception) {
                Log.d(TAG, "getClient: ${e}");
            }
        }

        return retrofitClient
    }
}