package com.shijian.app.ui.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class TabItem(val route: String, val label: String, val icon: ImageVector)

private val TABS = listOf(
    TabItem(Routes.HOME, "首页", Icons.Filled.Home),
    TabItem(Routes.FOOD, "美食", Icons.Filled.Restaurant),
    TabItem(Routes.EXPENSE, "开销", Icons.Filled.AccountBalanceWallet),
    TabItem(Routes.NEWS, "新闻", Icons.Filled.Newspaper),
    TabItem(Routes.PROFILE, "我的", Icons.Filled.Person)
)

/** 底部导航：贴底显示，顶部加一条细边避免与内容混淆 */
@Composable
fun SjBottomBar(
    currentRoute: String?,
    onTabSelected: (String) -> Unit
) {
    val selectedBase = currentRoute?.substringBefore('?')
    Surface(
        modifier = Modifier.navigationBarsPadding(),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.5.dp, Color(0x1F000000))
    ) {
        NavigationBar(
            modifier = Modifier.height(56.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            TABS.forEach { tab ->
                val selected = selectedBase == tab.route
                NavigationBarItem(
                    selected = selected,
                    onClick = {
                        val target = if (tab.route == Routes.EXPENSE) {
                            Routes.EXPENSE_FILTER.replace("{filter}", "")
                        } else {
                            tab.route
                        }
                        onTabSelected(target)
                    },
                    icon = {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            modifier = Modifier.height(22.dp)
                        )
                    },
                    label = {
                        Text(text = tab.label, fontSize = 11.sp, maxLines = 1)
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )
                )
            }
        }
    }
}
