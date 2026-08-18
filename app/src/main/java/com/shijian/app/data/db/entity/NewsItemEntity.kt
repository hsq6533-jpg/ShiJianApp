package com.shijian.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/** 新闻记录（3.6） */
@Entity(tableName = "news_items")
data class NewsItemEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val summary: String,
    /** 国内 / 国际 / 科技财经 / 民生 / 美食 / 体育 / 娱乐 */
    val category: String,
    val sourceHint: String = "DeepSeek 生成",
    val publishedAt: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    /** 是否命中特别关心 */
    val isSpecial: Boolean = false
)

/** 分类 → 标签配色（n-tag） */
val NEWS_CATEGORY_COLORS = mapOf(
    "国内" to 0xFF0A84FF.toInt(),
    "国际" to 0xFF5856D6.toInt(),
    "科技财经" to 0xFF34C759.toInt(),
    "民生" to 0xFFFF9500.toInt(),
    "美食" to 0xFFFF3B30.toInt(),
    "体育" to 0xFF00C7BE.toInt(),
    "娱乐" to 0xFFAF52DE.toInt()
)
