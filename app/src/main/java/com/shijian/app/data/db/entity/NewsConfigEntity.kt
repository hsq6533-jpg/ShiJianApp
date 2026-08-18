package com.shijian.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** 新闻设置（3.5），单行配置 */
@Entity(tableName = "news_config")
@Serializable
data class NewsConfigEntity(
    @PrimaryKey val id: Int = 1,
    /** 启用 AI 资讯 */
    val enabled: Boolean = true,
    /** 定时推送开关 */
    val pushEnabled: Boolean = false,
    /** DAILY / WEEKLY / MONTHLY */
    val pushFrequency: String = "DAILY",
    val pushHour: Int = 8,
    val pushMinute: Int = 0,
    /** WEEKLY 时生效：1-7（周一~周日） */
    val pushWeekday: Int = 1,
    /** MONTHLY 时生效：1-31 */
    val pushDay: Int = 1,
    /** 关注分类，逗号分隔 */
    val categories: String = "国内,国际,科技财经",
    /** 特别关心关键词，逗号分隔 */
    val specialKeywords: String = "",
    /** SHORT / MEDIUM / LONG */
    val contentLength: String = "MEDIUM",
    /** 上次成功更新时间戳 */
    val lastUpdatedAt: Long = 0
)
