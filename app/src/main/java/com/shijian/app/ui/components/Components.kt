package com.shijian.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shijian.app.ui.theme.TextPrimary
import com.shijian.app.ui.theme.TextSecondary

/** 卡片（设计稿：白底大圆角 19.2dp） */
@Composable
fun SjCard(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surface,
    onClick: (() -> Unit)? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    val shape = MaterialTheme.shapes.large
    Surface(
        modifier = modifier,
        shape = shape,
        color = color,
        shadowElevation = 0.dp
    ) {
        val m = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        Column(modifier = m.padding(16.dp), content = content)
    }
}

/** 卡片标题行 */
@Composable
fun CardTitle(
    title: String,
    trailing: String? = null,
    onTrailingClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (trailing != null) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelMedium,
                color = if (onTrailingClick != null) MaterialTheme.colorScheme.primary else TextSecondary,
                modifier = Modifier
                    .clickable(enabled = onTrailingClick != null) { onTrailingClick?.invoke() }
                    .padding(4.dp)
            )
        }
    }
}

/** 分组标题 */
@Composable
fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(horizontal = 20.dp, vertical = 12.dp)
    )
}

/** 可滚动页面外壳（含底部安全区） */
@Composable
fun ScreenScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable (() -> Unit)? = null,
    bottomBar: @Composable (() -> Unit)? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxSize()) {
        topBar?.invoke()
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            content = content
        )
        bottomBar?.invoke()
    }
}

/** 空状态 */
@Composable
fun EmptyState(
    emoji: String,
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = emoji, fontSize = 30.sp)
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (subtitle != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

/** 主按钮 */
@Composable
fun SjPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    gradient: Brush? = null
) {
    val bg = gradient ?: Brush.horizontalGradient(
        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary)
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(bg, MaterialTheme.shapes.small)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) Color.White else Color.White.copy(alpha = 0.5f),
            fontSize = 17.sp
        )
    }
}

/** 金额文本（等宽数字） */
@Composable
fun AmountText(
    text: String,
    color: Color = MaterialTheme.colorScheme.onSurface,
    fontSize: androidx.compose.ui.unit.TextUnit = 16.sp,
    fontWeight: FontWeight = FontWeight.Bold
) {
    Text(
        text = text,
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum")
    )
}

/** 分类彩色图标 */
@Composable
fun CategoryBadge(
    emoji: String,
    size: androidx.compose.ui.unit.Dp = 44.dp,
    background: Color = MaterialTheme.colorScheme.surfaceVariant,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(size).background(background, MaterialTheme.shapes.medium),
        contentAlignment = Alignment.Center
    ) {
        Text(text = emoji, fontSize = (size.value * 0.5f).sp)
    }
}

/** 小标签（待报销 / 奶茶 / 新闻分类） */
@Composable
fun SmallTag(
    text: String,
    background: Color,
    foreground: Color = Color.White,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = foreground,
        modifier = modifier
            .background(background, MaterialTheme.shapes.extraSmall)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

/** 横向滚动 chips 行 */
@Composable
fun ChipRow(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState(), reverseScrolling = true),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { opt ->
            val isSel = opt == selected
            Box(
                modifier = Modifier
                    .background(
                        if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        MaterialTheme.shapes.small
                    )
                    .clickable { onSelect(opt) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = opt,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSel) Color.White else TextPrimary
                )
            }
        }
    }
}

/** 设置/列表行 */
@Composable
fun ListRow(
    icon: String,
    label: String,
    value: String? = null,
    onClick: (() -> Unit)? = null,
    iconBackground: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryBadge(emoji = icon, size = 38.dp, background = iconBackground)
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (onClick != null) {
            Spacer(Modifier.width(4.dp))
            Text(text = "›", color = TextSecondary, fontSize = 20.sp)
        }
    }
}

/** 开关行 */
@Composable
fun SwitchRow(
    icon: String,
    label: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    iconBackground: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryBadge(emoji = icon, size = 38.dp, background = iconBackground)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
        androidx.compose.material3.Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

/** 分隔线 */
@Composable
fun Divider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .height(0.5.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}
