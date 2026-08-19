package com.shijian.app.ui.screens.profile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shijian.app.AppContainer
import com.shijian.app.data.prefs.AppSettings
import com.shijian.app.ui.components.ListRow
import com.shijian.app.ui.components.SjCard
import com.shijian.app.ui.components.SubPageTopBar
import com.shijian.app.ui.components.SwitchRow
import com.shijian.app.ui.navigation.Routes
import com.shijian.app.ui.theme.Brand100
import com.shijian.app.ui.theme.Brand500
import com.shijian.app.ui.theme.Danger500
import com.shijian.app.ui.theme.TextSecondary
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

@Composable
fun FoodSettingsScreen(
    container: AppContainer,
    onBack: () -> Unit,
    nav: (String) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val settings by remember {
        container.settingsRepo.settings
            .catch { emit(AppSettings()) }
    }.collectAsStateWithLifecycle(initialValue = AppSettings())

    var amapKey by remember { mutableStateOf("") }
    var showClear by remember { mutableStateOf(false) }
    var showFetchDialog by remember { mutableStateOf(false) }

    val toast: (String) -> Unit = { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }
    val keyConfigured = remember { container.securePrefs.getAmapKey() != null }

    LaunchedEffect(Unit) {
        amapKey = container.securePrefs.getAmapKey().orEmpty()
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        SubPageTopBar(title = "美食设置", onBack = onBack)

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            GroupTitle("高德 Key")
            SjCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (keyConfigured) "✓ 已配置 · 用于搜索附近美食，仅保存在本机" else "未配置 · 用于搜索附近美食，仅保存在本机",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (keyConfigured) Brand500 else TextSecondary
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = amapKey,
                    onValueChange = { amapKey = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("输入高德 Web 服务 Key") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Brand500)
                )
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(999.dp))
                        .clickable {
                            container.securePrefs.setAmapKey(amapKey.trim().ifEmpty { null })
                            toast(if (amapKey.isNotBlank()) "高德 Key 已保存" else "已清空 Key")
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("保存 Key", color = Color.White, style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(Modifier.height(16.dp))

            GroupTitle("搜索范围")
            SjCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "范围越大结果越全，调用量也越大",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${settings.searchRadiusKm}",
                        color = Brand500,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum")
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = " 公里",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Slider(
                    value = settings.searchRadiusKm.toFloat(),
                    onValueChange = { container.settingsRepo.setSearchRadiusKm(it.toInt().coerceIn(1, 10)) },
                    valueRange = 1f..10f,
                    steps = 8
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("1 km", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text("5 km", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text("10 km", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
            }

            Spacer(Modifier.height(16.dp))

            GroupTitle("数据管理")
            SjCard(modifier = Modifier.fillMaxWidth()) {
                ListRow(
                    icon = "🍽️",
                    label = "手动获取周围美食店铺",
                    value = "获取所有附近美食",
                    onClick = { showFetchDialog = true },
                    iconBackground = Brand100
                )
                ListRow(
                    icon = "🗺️",
                    label = "地址管理",
                    value = "常用地址",
                    onClick = { nav(Routes.ADDRESS_MANAGE) },
                    iconBackground = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            Spacer(Modifier.height(16.dp))

            GroupTitle("其他")
            SjCard(modifier = Modifier.fillMaxWidth()) {
                SwitchRow(
                    icon = "📍",
                    label = "多点位搜索",
                    subtitle = "覆盖更大区域，结果更全（推荐开启）",
                    checked = settings.multiPointSearch,
                    onCheckedChange = { container.settingsRepo.setMultiPointSearch(it) },
                    iconBackground = Brand100
                )
            }

            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(13.dp))
                    .clickable { showClear = true },
                contentAlignment = Alignment.Center
            ) {
                Text("🗑 清除美食缓存", style = MaterialTheme.typography.labelLarge, color = Danger500)
            }
            Spacer(Modifier.height(24.dp))
            Text(
                text = "时笺手机版 · 纯本地运行",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
        }
    }

    if (showClear) {
        AlertDialog(
            onDismissRequest = { showClear = false },
            title = { Text("清除美食缓存？") },
            text = { Text("将删除本地缓存的搜索记录（保留收藏与拉黑标记），下次搜索需重新请求。") },
            confirmButton = {
                TextButton(onClick = {
                    showClear = false
                    scope.launch {
                        runCatching {
                            container.foodRepo.clearCache()
                        }.onSuccess { toast("美食缓存已清除") }
                            .onFailure { toast("清除失败，请重试") }
                    }
                }) { Text("清除", color = Danger500) }
            },
            dismissButton = {
                TextButton(onClick = { showClear = false }) { Text("取消") }
            }
        )
    }

    if (showFetchDialog) {
        ManualFetchDialog(
            container = container,
            onDismiss = { showFetchDialog = false },
            onComplete = { count ->
                toast("获取完成，共 ${count} 家店铺")
                showFetchDialog = false
            }
        )
    }
}

@Composable
private fun ManualFetchDialog(
    container: AppContainer,
    onDismiss: () -> Unit,
    onComplete: (Int) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val key = container.securePrefs.getAmapKey()
    var fetching by remember { mutableStateOf(false) }
    var pageCount by remember { mutableStateOf(0) }
    var errorMsg by remember { mutableStateOf("") }

    val settings by remember {
        container.settingsRepo.settings
            .catch { emit(AppSettings()) }
    }.collectAsStateWithLifecycle(initialValue = AppSettings())

    var lat by remember { mutableStateOf(0.0) }
    var lng by remember { mutableStateOf(0.0) }

    AlertDialog(
        onDismissRequest = { if (!fetching) onDismiss() },
        title = { Text("手动获取周围美食店铺") },
        text = {
            Column {
                Text(
                    text = "将获取当前位置范围内所有美食店铺，缓存到本地供离线使用。",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(Modifier.height(12.dp))

                if (fetching) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "正在获取… 第 ${pageCount} 页",
                        style = MaterialTheme.typography.bodySmall
                    )
                } else if (errorMsg.isNotBlank()) {
                    Text(
                        text = errorMsg,
                        color = Danger500,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Text(
                        text = "搜索范围：${settings.searchRadiusKm} 公里",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "点击「开始获取」将自动定位并抓取所有美食店铺。",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (key.isNullOrBlank()) {
                        errorMsg = "请先配置高德 Key"
                        return@TextButton
                    }
                    fetching = true
                    pageCount = 0
                    errorMsg = ""
                    scope.launch {
                        val loc = runCatching {
                            val lm = android.location.LocationManager::class.java
                            val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
                            val providers = locationManager.getProviders(true)
                            for (provider in providers) {
                                try {
                                    val lastLoc = locationManager.getLastKnownLocation(provider)
                                    if (lastLoc != null) {
                                        lat = lastLoc.latitude
                                        lng = lastLoc.longitude
                                        break
                                    }
                                } catch (_: SecurityException) {}
                            }
                            if (lat == 0.0) {
                                val fallback = com.shijian.app.util.LocationUtils.getCurrentLocation(context)
                                if (fallback != null) {
                                    lat = fallback.latitude
                                    lng = fallback.longitude
                                }
                            }
                            lat to lng
                        }.getOrNull()

                        if (lat == 0.0) {
                            errorMsg = "无法获取当前位置，请确保已开启定位"
                            fetching = false
                            return@launch
                        }

                        val result = container.foodRepo.fetchAllAround(key, lat, lng, settings.searchRadiusKm) { page, _ ->
                            pageCount = page
                        }
                        container.settingsRepo.setLastSearchLatLng(lat, lng)
                        val count = container.foodRepo.results.value.size
                        fetching = false
                        if (result) {
                            onComplete(count)
                        } else {
                            errorMsg = "获取失败，请检查 Key 或网络"
                        }
                    }
                },
                enabled = !fetching
            ) { Text(if (fetching) "获取中…" else "开始获取") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !fetching) { Text("取消") }
        }
    )
}

@Composable
private fun GroupTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = TextSecondary,
        modifier = Modifier.padding(start = 4.dp, top = 18.dp, bottom = 8.dp)
    )
}
