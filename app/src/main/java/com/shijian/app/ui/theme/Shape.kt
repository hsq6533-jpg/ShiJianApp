package com.shijian.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** 与设计稿一致的圆角体系 */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),   // chips / 小标签
    small = RoundedCornerShape(12.dp),       // 输入框 / 按钮
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(19.2.dp),     // 卡片
    extraLarge = RoundedCornerShape(24.dp)
)
