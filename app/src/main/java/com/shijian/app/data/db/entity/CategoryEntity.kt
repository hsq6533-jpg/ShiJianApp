package com.shijian.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** 账单分类（3.2） */
@Entity(tableName = "categories")
@Serializable
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** EXPENSE / INCOME */
    val type: String,
    /** 本地 emoji 图标 */
    val icon: String,
    val sortOrder: Int = 0,
    val isBuiltIn: Boolean = true
)

/** 默认支出分类 */
val DEFAULT_EXPENSE_CATEGORIES = listOf(
    "餐饮" to "🍜",
    "奶茶" to "☕",
    "交通" to "🚌",
    "购物" to "🛍️",
    "娱乐" to "🎮",
    "医疗" to "💊",
    "教育" to "📚",
    "其他" to "📦"
)

/** 默认收入分类 */
val DEFAULT_INCOME_CATEGORIES = listOf(
    "工资" to "💰",
    "奖金" to "🏆",
    "投资" to "📈",
    "兼职" to "💼",
    "红包" to "🧧",
    "其他" to "📦"
)
