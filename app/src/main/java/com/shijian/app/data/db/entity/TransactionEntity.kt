package com.shijian.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** 账单记录（3.1） */
@Entity(tableName = "transactions")
@Serializable
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** EXPENSE / INCOME */
    val type: String,
    val amount: Double,
    /** 分类名，如 奶茶/餐饮/工资 */
    val category: String,
    val remark: String = "",
    /** 商家 / 来源 */
    val merchant: String = "",
    /** yyyy-MM-dd */
    val date: String,
    /** HH:mm */
    val time: String,
    /** 是否待报销 */
    val isReimbursable: Boolean = false,
    /** 是否已报销 */
    val isReimbursed: Boolean = false,
    /** 是否奶茶（分类为奶茶时自动为 true） */
    val isMilkTea: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

const val TYPE_EXPENSE = "EXPENSE"
const val TYPE_INCOME = "INCOME"
