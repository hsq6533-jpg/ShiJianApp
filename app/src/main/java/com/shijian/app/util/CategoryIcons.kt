package com.shijian.app.util

import com.shijian.app.data.db.entity.DEFAULT_EXPENSE_CATEGORIES
import com.shijian.app.data.db.entity.DEFAULT_INCOME_CATEGORIES

/** 分类名 → emoji 图标 */
private val CATEGORY_EMOJI: Map<String, String> =
    (DEFAULT_EXPENSE_CATEGORIES + DEFAULT_INCOME_CATEGORIES).toMap()

fun categoryEmoji(name: String): String = CATEGORY_EMOJI[name] ?: "📦"
