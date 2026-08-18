package com.shijian.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** 美食 POI（3.3） */
@Entity(tableName = "food_pois")
@Serializable
data class FoodPoiEntity(
    /** 高德 POI ID */
    @PrimaryKey val id: String,
    val name: String,
    /** 类型：火锅、烧烤… */
    val type: String,
    val address: String,
    val longitude: Double,
    val latitude: Double,
    /** 米 */
    val distance: Int = 0,
    val rating: Float? = null,
    /** 人均消费 */
    val cost: Int? = null,
    /** 图片 URL 列表，JSON 数组字符串 */
    val photos: String = "[]",
    val source: String = "amap",
    /** 搜索中心点标识（城市-经纬度-半径） */
    val searchCenter: String = "",
    val cachedAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val isBlacklisted: Boolean = false
)
