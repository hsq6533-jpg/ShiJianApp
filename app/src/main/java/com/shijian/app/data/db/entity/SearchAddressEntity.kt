package com.shijian.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** 搜索地址（3.4） */
@Entity(tableName = "search_addresses")
@Serializable
data class SearchAddressEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** 如 家 / 公司 */
    val name: String,
    val address: String,
    val longitude: Double? = null,
    val latitude: Double? = null,
    val isDefault: Boolean = false,
    val sortOrder: Int = 0
)
