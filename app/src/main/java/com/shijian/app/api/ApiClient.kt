package com.shijian.app.api

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.shijian.app.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/** Retrofit 工厂（高德 / DeepSeek） */
object ApiClient {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private fun client(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            // 注意：URL 可能携带高德 key，release 关闭日志
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
            else HttpLoggingInterceptor.Level.NONE
        }
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    private val contentType = "application/json".toMediaType()

    val amap: AmapService by lazy {
        Retrofit.Builder()
            .baseUrl("https://restapi.amap.com/")
            .client(client())
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(AmapService::class.java)
    }

    val deepseek: DeepSeekApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.deepseek.com/")
            .client(client())
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(DeepSeekApi::class.java)
    }
}
