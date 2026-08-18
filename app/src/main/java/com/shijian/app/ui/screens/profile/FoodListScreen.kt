package com.shijian.app.ui.screens.profile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shijian.app.AppContainer
import com.shijian.app.data.db.entity.FoodPoiEntity
import com.shijian.app.ui.components.EmptyState
import com.shijian.app.ui.components.SubPageTopBar
import com.shijian.app.ui.theme.Brand500
import com.shijian.app.ui.theme.Danger100
import com.shijian.app.ui.theme.Danger500
import com.shijian.app.ui.theme.Orange500
import com.shijian.app.ui.theme.TextSecondary
import com.shijian.app.util.FormatUtils
import kotlinx.coroutines.launch

/** 收藏 / 拉黑管理（我的页 → 美食设置，PRD 4.5.3） */
@Composable
fun FoodListScreen(
    container: AppContainer,
    type: String,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val isFavs = type == "favorites"
    val list = if (isFavs)
        container.foodRepo.observeFavorites().collectAsStateWithLifecycle(initialValue = emptyList())
    else
        container.foodRepo.observeBlacklisted().collectAsStateWithLifecycle(initialValue = emptyList())

    val toast: (String) -> Unit = { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }

    Column(modifier = Modifier.fillMaxWidth()) {
        SubPageTopBar(
            title = if (isFavs) "收藏管理" else "拉黑管理",
            onBack = onBack
        )
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            if (list.value.isEmpty()) {
                EmptyState(
                    emoji = if (isFavs) "❤️" else "🚫",
                    title = if (isFavs) "还没有收藏的美食" else "拉黑列表为空",
                    subtitle = if (isFavs)
                        "在美食页结果中点击 ❤️ 即可收藏"
                    else
                        "在美食页结果中点击 🚫 即可拉黑不再推荐"
                )
            } else {
                Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)) {
                    list.value.forEach { poi ->
                        PoiManageCard(
                            poi = poi,
                            isFavs = isFavs,
                            onAction = {
                                scope.launch {
                                    if (isFavs) {
                                        container.foodRepo.toggleFavorite(poi)
                                        toast("已取消收藏「${poi.name}」")
                                    } else {
                                        container.foodRepo.setBlacklisted(poi, false)
                                        toast("已解除拉黑「${poi.name}」")
                                    }
                                }
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun PoiManageCard(
    poi: FoodPoiEntity,
    isFavs: Boolean,
    onAction: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.large)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(if (isFavs) Danger100 else MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = foodEmoji(poi.type), fontSize = 20.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = poi.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${poi.type} · ${poi.address.ifBlank { "地址未知" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (poi.rating != null || poi.cost != null) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = listOfNotNull(
                            poi.rating?.let { "⭐ ${FormatUtils.rating(it)}" },
                            poi.cost?.let { "¥${it}/人" }
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .background(
                        if (isFavs) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(999.dp)
                    )
                    .clickable(onClick = onAction)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (isFavs) "取消收藏" else "解除拉黑",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isFavs) Brand500 else Orange500,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private fun foodEmoji(type: String): String = when {
    type.contains("火锅") -> "🍲"
    type.contains("烧烤") || type.contains("烤肉") -> "🍢"
    type.contains("面") || type.contains("粉") || type.contains("馄饨") || type.contains("饺子") || type.contains("包子") -> "🍜"
    type.contains("甜品") || type.contains("蛋糕") || type.contains("面包") || type.contains("烘焙") || type.contains("冰淇淋") -> "🍰"
    type.contains("奶茶") || type.contains("咖啡") || type.contains("饮品") -> "🧋"
    type.contains("快餐") || type.contains("汉堡") || type.contains("披萨") -> "🍔"
    type.contains("日") || type.contains("寿司") || type.contains("韩") -> "🍣"
    type.contains("西餐") -> "🍕"
    type.contains("小吃") || type.contains("粤") -> "🥟"
    type.contains("海鲜") || type.contains("水产") -> "🦐"
    type.contains("水果") || type.contains("鲜果") -> "🍉"
    else -> "🍽️"
}
