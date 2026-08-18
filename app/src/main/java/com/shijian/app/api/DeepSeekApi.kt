package com.shijian.app.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeepSeekMessage(
    @SerialName("role") val role: String,
    @SerialName("content") val content: String
)

@Serializable
data class DeepSeekChatRequest(
    @SerialName("model") val model: String = "deepseek-chat",
    @SerialName("messages") val messages: List<DeepSeekMessage>,
    @SerialName("temperature") val temperature: Double = 0.8,
    @SerialName("response_format") val responseFormat: ResponseFormat = ResponseFormat("json_object")
)

@Serializable
data class ResponseFormat(@SerialName("type") val type: String)

@Serializable
data class DeepSeekChoice(@SerialName("message") val message: DeepSeekMessage)

@Serializable
data class DeepSeekResponse(
    @SerialName("choices") val choices: List<DeepSeekChoice> = emptyList()
)

/** DeepSeek Chat Completions（7.2） */
interface DeepSeekApi {

    @retrofit2.http.POST("chat/completions")
    suspend fun chat(
        @retrofit2.http.Header("Authorization") authorization: String,
        @retrofit2.http.Body body: DeepSeekChatRequest
    ): DeepSeekResponse
}
