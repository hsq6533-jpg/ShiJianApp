package com.shijian.app.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AmapPhoto(
    @SerialName("title") val title: String = "",
    @SerialName("url") val url: String = ""
)

/** 高德 POI（v5 place 返回项） */
@Serializable
data class AmapPoi(
    @SerialName("id") val id: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("type") val type: String = "",
    @SerialName("address") val address: String = "",
    /** "lng,lat" */
    @SerialName("location") val location: String = "",
    @SerialName("distance") val distance: Int? = null,
    @SerialName("rating") val rating: Float? = null,
    @SerialName("cost") val cost: Int? = null,
    @SerialName("photos") val photos: List<AmapPhoto>? = null
)

@Serializable
data class AmapAroundResp(
    @SerialName("status") val status: String = "0",
    @SerialName("info") val info: String = "",
    @SerialName("pois") val pois: List<AmapPoi> = emptyList()
)

@Serializable
data class AmapTextResp(
    @SerialName("status") val status: String = "0",
    @SerialName("info") val info: String = "",
    @SerialName("pois") val pois: List<AmapPoi> = emptyList()
)

@Serializable
data class AmapGeocode(
    @SerialName("formatted_address") val formattedAddress: String = "",
    /** "lng,lat" */
    @SerialName("location") val location: String = ""
)

@Serializable
data class AmapGeocodeResp(
    @SerialName("status") val status: String = "0",
    @SerialName("geocodes") val geocodes: List<AmapGeocode> = emptyList()
)

/**
 * 高德 Web 服务 API（7.1）
 * 文档：https://lbs.amap.com/api/webservice/guide/api-advanced
 */
interface AmapService {

    /** 周边搜索 v5 */
    @retrofit2.http.GET("v5/place/around")
    suspend fun around(
        @retrofit2.http.Query("key") key: String,
        @retrofit2.http.Query("location") location: String,
        @retrofit2.http.Query("radius") radius: Int,
        @retrofit2.http.Query("keywords") keywords: String? = null,
        @retrofit2.http.Query("types") types: String? = null,
        @retrofit2.http.Query("sortrule") sortrule: String? = null,
        @retrofit2.http.Query("offset") offset: Int = 25,
        @retrofit2.http.Query("page") page: Int = 1
    ): AmapAroundResp

    /** 关键词搜索 v5 */
    @retrofit2.http.GET("v5/place/text")
    suspend fun text(
        @retrofit2.http.Query("key") key: String,
        @retrofit2.http.Query("keywords") keywords: String,
        @retrofit2.http.Query("city") city: String? = null,
        @retrofit2.http.Query("offset") offset: Int = 25,
        @retrofit2.http.Query("page") page: Int = 1
    ): AmapTextResp

    /** 地理编码（地址 → 坐标）v3 */
    @retrofit2.http.GET("v3/geocode/geo")
    suspend fun geocode(
        @retrofit2.http.Query("key") key: String,
        @retrofit2.http.Query("address") address: String,
        @retrofit2.http.Query("city") city: String? = null
    ): AmapGeocodeResp
}
