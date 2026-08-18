@file:OptIn(ExperimentalFoundationApi::class)

package com.shijian.app.ui.screens.food

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shijian.app.AppContainer
import com.shijian.app.data.prefs.AppSettings
import com.shijian.app.data.db.entity.FoodPoiEntity
import com.shijian.app.data.db.entity.SearchAddressEntity
import com.shijian.app.data.repo.FoodRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import com.shijian.app.ui.components.EmptyState
import com.shijian.app.ui.components.SjCard
import com.shijian.app.ui.components.TabTopBar
import com.shijian.app.ui.navigation.Routes
import com.shijian.app.ui.theme.Brand500
import com.shijian.app.ui.theme.Danger500
import com.shijian.app.ui.theme.Orange500
import com.shijian.app.ui.theme.TextSecondary
import com.shijian.app.util.AmapIntents
import com.shijian.app.util.FormatUtils
import com.shijian.app.util.LocationUtils
import kotlinx.coroutines.launch

private val SORT_OPTIONS = listOf("综合", "距离最近", "评分最高")

/** 地址特征词：命中则按地址解析周边搜索，否则按关键词搜索 */
private val ADDRESS_KEYWORDS = listOf(
    "路", "街", "道", "巷", "村", "镇", "区", "号",
    "大厦", "广场", "中心", "小区", "公寓", "酒店", "公园", "桥", "门", "大道"
)

private fun isAddressQuery(q: String): Boolean {
    val s = q.trim()
    if (s.length < 3) return false
    return ADDRESS_KEYWORDS.any { s.contains(it) }
}

/** 搜索中心：定位坐标或地址 */
private data class SearchCenter(
    val label: String,
    val lat: Double? = null,
    val lng: Double? = null,
    val address: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodScreen(
    container: AppContainer,
    nav: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val settings by remember {
        container.settingsRepo.settings
            .catch { emit(AppSettings()) }
    }.collectAsStateWithLifecycle(initialValue = AppSettings())
    val addresses by remember {
        container.addressRepo.observeAll()
            .catch { emit(emptyList()) }
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    val results by remember {
        container.foodRepo.results
            .catch { emit(emptyList()) }
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    val searching by remember {
        container.foodRepo.searching
            .catch { emit(false) }
    }.collectAsStateWithLifecycle(initialValue = false)
    val error by remember {
        container.foodRepo.error
            .catch { emit(null) }
    }.collectAsStateWithLifecycle(initialValue = null)

    // ---- 状态 ----
    var amapKey by remember { mutableStateOf(container.securePrefs.getAmapKey()) }
    var showKeySheet by remember { mutableStateOf(false) }
    var keyInput by rememberSaveable { mutableStateOf("") }

    var kw by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("全部") }
    var sort by rememberSaveable { mutableStateOf("综合") }
    var hasSearched by rememberSaveable { mutableStateOf(false) }

    var center by remember { mutableStateOf<SearchCenter?>(null) }
    var showCenterPicker by remember { mutableStateOf(false) }
    var pickingLocation by remember { mutableStateOf(false) }

    var picked by remember { mutableStateOf<FoodPoiEntity?>(null) }
    var picking by remember { mutableStateOf(false) }
    var blacklistTarget by remember { mutableStateOf<FoodPoiEntity?>(null) }

    // 首次进入：自动定位为搜索中心（定位不可用则用默认地址），定位完成后自动随机推荐
    var locatedOnce by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (locatedOnce) return@LaunchedEffect
        locatedOnce = true
        var c: SearchCenter? = null
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            pickingLocation = true
            val loc = LocationUtils.getCurrentLocation(context)
            pickingLocation = false
            if (loc != null) c = SearchCenter("我的定位", loc.latitude, loc.longitude)
        }
        if (c == null) {
            val def = runCatching { container.addressRepo.getDefault() }.getOrNull()
            if (def != null) c = def.toCenter()
        }
        center = c
        runCatching { pick() }
    }

    // 随机推荐：先确保有定位中心 → 周边搜索 → 排除奶茶甜品 → 随机挑一个
    val pick: () -> Unit = {
        picking = true
        scope.launch {
            runCatching {
                val key = container.securePrefs.getAmapKey()
                var c = center
                if (c == null || c.lat == null || c.lng == null) {
                    val loc = LocationUtils.getCurrentLocation(context)
                    if (loc != null) {
                        c = SearchCenter("我的定位", loc.latitude, loc.longitude)
                        center = c
                    }
                }
                if (c?.lat != null && c.lng != null && !key.isNullOrBlank()) {
                    // 周边搜索（半径用设置里的预设距离）
                    container.foodRepo.searchAround(
                        key, c.lat!!, c.lng!!,
                        settings.searchRadiusKm, null, null, settings.multiPointSearch
                    )
                    val all = container.foodRepo.results.value.filter { !it.isBlacklisted }
                    // 优先非奶茶/甜品/咖啡/饮品类
                    val preferred = all.filter { poi ->
                        val t = poi.type + poi.name
                        !(t.contains("奶茶") || t.contains("甜品") || t.contains("咖啡") ||
                            t.contains("饮品") || t.contains("蛋糕") || t.contains("面包") ||
                            t.contains("冰淇淋") || t.contains("茶艺") || t.contains("果饮"))
                    }
                    picked = (preferred.ifEmpty { all }).randomOrNull()
                } else {
                    // 无中心或未配置 Key：从本地缓存随机
                    picked = container.foodRepo.randomPick()
                }
            }
            picking = false
        }
    }

    val toast: (String) -> Unit = { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }

    // 定位权限申请（用户手动触发时）
    var useGps: () -> Unit = {}
    val locationPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            useGps()
        } else {
            toast("未授予定位权限，请在设置中允许后使用定位")
        }
    }

    useGps = {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            locationPermLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            scope.launch {
                pickingLocation = true
                val loc = LocationUtils.getCurrentLocation(context)
                pickingLocation = false
                if (loc != null) {
                    center = SearchCenter("我的定位", loc.latitude, loc.longitude)
                } else {
                    toast("定位失败，请检查定位是否开启")
                }
            }
        }
    }

    // 统一搜索：智能识别「地址」或「关键词」，都在预设距离内搜索
    val doSearch: () -> Unit = doSearch@ {
        val key = container.securePrefs.getAmapKey()
        if (key.isNullOrBlank()) {
            showKeySheet = true
            toast("请先配置高德 Key")
            return@doSearch
        }
        hasSearched = true
        scope.launch {
            runCatching {
                val q = kw.trim()
                val c = center
                when {
                    // 1. 地址 → 解析坐标后搜周边
                    isAddressQuery(q) -> container.foodRepo.searchByAddress(
                        key, q, null,
                        settings.searchRadiusKm, settings.multiPointSearch
                    )
                    // 2. 有关键词或空关键词 + 有定位中心 → 周边搜索
                    c?.lat != null && c.lng != null -> container.foodRepo.searchAround(
                        key, c.lat!!, c.lng!!,
                        settings.searchRadiusKm, q.ifBlank { null }, null, settings.multiPointSearch
                    )
                    // 3. 兜底：无中心 → 全国关键词搜索
                    else -> container.foodRepo.searchByKeyword(key, q, null)
                }
            }.onFailure { toast("搜索失败，请检查 Key 或网络") }
        }
    }

    val displayed = remember(results, category, sort) {
        val base = results.filter { !it.isBlacklisted }
        val filtered = if (category == "全部") base
        else base.filter { it.type.contains(category) || it.name.contains(category) }
        when (sort) {
            "距离最近" -> filtered.sortedBy { it.distance }
            "评分最高" -> filtered.sortedByDescending { it.rating ?: -1f }
            else -> filtered
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        TabTopBar(
            title = "美食",
            actions = {
                Box {
                    IconButton(onClick = { showKeySheet = true; keyInput = amapKey.orEmpty() }) {
                        Icon(Icons.Filled.LocationOn, contentDescription = "附近美食设置", tint = MaterialTheme.colorScheme.onBackground)
                    }
                    if (amapKey.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .background(Danger500, CircleShape)
                                .align(Alignment.TopEnd)
                        )
                    }
                }
            }
        )

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // ---- 随机推荐卡 ----
            RandomHeroCard(picked = picked, picking = picking, onRandom = pick) { p ->
                AmapIntents.openNavigation(context, p.latitude, p.longitude, p.name)
            }

            Spacer(Modifier.height(14.dp))

            // ---- 统一搜索框（智能识别地址/关键词） ----
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = kw,
                    onValueChange = { kw = it },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    placeholder = {
                        Text(
                            "搜附近美食，或输入地址/关键词",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Brand500)
                )
                Box(
                    modifier = Modifier
                        .height(40.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(999.dp))
                        .clickable(onClick = doSearch)
                        .padding(horizontal = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searching) "搜索中…" else "搜索",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "输入地址搜周边美食；输入奶茶、汉堡等关键词搜附近相关店铺；留空搜全部",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                maxLines = 2
            )

            Spacer(Modifier.height(10.dp))

            // ---- 搜索中心 ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brand500.copy(alpha = 0.10f), RoundedCornerShape(999.dp))
                    .clickable { showCenterPicker = true }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Brand500, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = when {
                        pickingLocation -> "正在定位…"
                        center == null -> "选择搜索中心（当前定位或常用地址）"
                        center?.label == "我的定位" -> "当前定位 · 半径 ${settings.searchRadiusKm}km"
                        else -> "中心：${center?.label} · 半径 ${settings.searchRadiusKm}km"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (center == null) TextSecondary else Brand500,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(text = "›", color = TextSecondary, fontSize = 18.sp)
            }

            // ---- 分类 chips ----
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(FoodRepository.FOOD_CHIPS.size) { i ->
                    val c = FoodRepository.FOOD_CHIPS[i]
                    val sel = c == category
                    Box(
                        modifier = Modifier
                            .background(
                                if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                RoundedCornerShape(999.dp)
                            )
                            .clickable { category = c }
                            .padding(horizontal = 13.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = c,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (sel) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ---- 排序 chips ----
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SORT_OPTIONS.forEach { s ->
                    val sel = s == sort
                    Box(
                        modifier = Modifier
                            .background(
                                if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                RoundedCornerShape(999.dp)
                            )
                            .clickable { sort = s }
                            .padding(horizontal = 13.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = s,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (sel) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // ---- 结果区 ----
            Text(
                text = if (kw.isNotBlank()) "「${kw.trim()}」搜索结果" else "附近美食",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = when {
                    searching -> "正在搜索美食…"
                    error != null -> error!!
                    displayed.isNotEmpty() -> "共 ${displayed.size} 家店铺 · 来自高德 · 缓存 30 天"
                    !hasSearched -> if (amapKey.isNullOrBlank()) "配置高德 Key 后即可搜索附近美食" else "进入页面已按当前定位搜索，也可以输入地址或关键词再搜"
                    else -> "没有找到相关店铺，换个关键词或范围试试"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (error != null) Danger500 else TextSecondary
            )

            if (displayed.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    displayed.forEach { poi ->
                        FoodPoiCard(
                            poi = poi,
                            onFav = {
                                scope.launch { container.foodRepo.toggleFavorite(poi) }
                            },
                            onMap = {
                                AmapIntents.openNavigation(context, poi.latitude, poi.longitude, poi.name)
                            },
                            onBlock = { blacklistTarget = poi },
                            onAddressClick = {
                                AmapIntents.openNavigation(context, poi.latitude, poi.longitude, poi.name)
                            },
                            onAddressLongPress = {
                                if (poi.address.isNotBlank()) {
                                    clipboard.setText(AnnotatedString(poi.address))
                                    toast("已复制地址")
                                }
                            }
                        )
                    }
                }
            } else {
                EmptyState(
                    emoji = "🍽️",
                    title = if (amapKey.isNullOrBlank()) "尚未配置高德 Key" else "暂无美食数据",
                    subtitle = if (amapKey.isNullOrBlank()) "点击右上角定位图标配置" else "换个关键词或范围试试"
                )
            }

            Spacer(Modifier.height(22.dp))
            Text(
                text = "时笺手机版 · 纯本地运行",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
        }
    }

    // ---- 搜索中心选择弹窗 ----
    if (showCenterPicker) {
        SearchCenterDialog(
            addresses = addresses,
            current = center,
            locating = pickingLocation,
            onDismiss = { showCenterPicker = false },
            onGps = { useGps(); showCenterPicker = false },
            onPickAddress = { a ->
                center = a.toCenter()
                showCenterPicker = false
            },
            onManage = {
                showCenterPicker = false
                nav(Routes.ADDRESS_MANAGE)
            }
        )
    }

    // ---- 高德 Key 设置面板 ----
    if (showKeySheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(onDismissRequest = { showKeySheet = false }, sheetState = sheetState) {
            Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
                Text(text = "附近美食设置", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "去 https://lbs.amap.com 申请 Web 服务 Key（免费）",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = keyInput,
                    onValueChange = { keyInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("高德 Web 服务 API Key") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Brand500)
                )
                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(999.dp))
                        .clickable {
                            container.securePrefs.setAmapKey(keyInput.trim().ifEmpty { null })
                            amapKey = container.securePrefs.getAmapKey()
                            showKeySheet = false
                            toast(if (amapKey != null) "Key 已保存" else "已清空 Key")
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "保存 Key", color = Color.White, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }

    // ---- 拉黑确认 ----
    blacklistTarget?.let { poi ->
        AlertDialog(
            onDismissRequest = { blacklistTarget = null },
            title = { Text("拉黑该店铺？") },
            text = { Text("「${poi.name}」将不再出现在搜索和随机推荐中。") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { container.foodRepo.setBlacklisted(poi, true) }
                    blacklistTarget = null
                }) { Text("拉黑", color = Danger500) }
            },
            dismissButton = {
                TextButton(onClick = { blacklistTarget = null }) { Text("取消") }
            }
        )
    }
}

// ==================== 随机推荐卡 ====================
@Composable
private fun RandomHeroCard(
    picked: FoodPoiEntity?,
    picking: Boolean,
    onRandom: () -> Unit,
    onGo: (FoodPoiEntity) -> Unit
) {
    SjCard(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
    ) {
        Text(
            text = "● 随机推荐",
            style = MaterialTheme.typography.labelMedium,
            color = Brand500,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(6.dp))
        Text(text = "今天吃什么？", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))
        Text(text = "本地美食 · 帮你快速做决定", style = MaterialTheme.typography.bodySmall, color = TextSecondary)

        if (picked != null) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = picked.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "${picked.type} · ${FormatUtils.distance(picked.distance)} · ${FormatUtils.cost(picked.cost)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Box(
                    modifier = Modifier
                        .background(Brand500, RoundedCornerShape(10.dp))
                        .clickable { onGo(picked) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(text = "去这里", color = Color.White, style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .background(Brand500, RoundedCornerShape(999.dp))
                    .clickable { onRandom() }
                    .padding(horizontal = 18.dp, vertical = 11.dp)
            ) {
                Text(
                    text = if (picking) "挑选中…" else "随机吃一个",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                    .clickable { onRandom() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Shuffle, contentDescription = "换一换", tint = Brand500, modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ==================== 店铺卡片 ====================
@Composable
private fun FoodPoiCard(
    poi: FoodPoiEntity,
    onFav: () -> Unit,
    onMap: () -> Unit,
    onBlock: () -> Unit,
    onAddressClick: () -> Unit,
    onAddressLongPress: () -> Unit
) {
    SjCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Brand500.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = foodEmoji(poi.type), fontSize = 22.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = poi.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    poi.rating?.let {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "⭐ ${FormatUtils.rating(it)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Orange500
                        )
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "${poi.type} · ${FormatUtils.distance(poi.distance)} · ${FormatUtils.cost(poi.cost)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .combinedClickable(onClick = onAddressClick, onLongClick = onAddressLongPress)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(5.dp))
            Text(
                text = poi.address.ifBlank { "暂无详细地址" },
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(text = "点击导航 · 长按复制", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionPill(icon = { Icon(if (poi.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, contentDescription = null, tint = if (poi.isFavorite) Danger500 else MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(15.dp)) }, text = "收藏", tint = if (poi.isFavorite) Danger500 else null, onClick = onFav)
            ActionPill(icon = { Icon(Icons.Filled.Map, contentDescription = null, tint = Brand500, modifier = Modifier.size(15.dp)) }, text = "地图", tint = Brand500, onClick = onMap)
            ActionPill(icon = { Icon(Icons.Filled.Block, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(15.dp)) }, text = "拉黑", tint = TextSecondary, onClick = onBlock)
        }
    }
}

@Composable
private fun RowScope.ActionPill(
    icon: @Composable () -> Unit,
    text: String,
    tint: Color?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .weight(1f)
            .background(
                tint?.copy(alpha = 0.10f) ?: MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                RoundedCornerShape(999.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(Modifier.width(5.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = tint ?: MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ==================== 搜索中心选择 ====================
@Composable
private fun SearchCenterDialog(
    addresses: List<SearchAddressEntity>,
    current: SearchCenter?,
    locating: Boolean,
    onDismiss: () -> Unit,
    onGps: () -> Unit,
    onPickAddress: (SearchAddressEntity) -> Unit,
    onManage: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择搜索中心") },
        text = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brand500.copy(alpha = 0.10f), RoundedCornerShape(10.dp))
                        .clickable(onClick = onGps)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Brand500, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (locating) "正在定位…" else "当前定位",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Brand500
                    )
                    if (current?.label == "我的定位") {
                        Spacer(Modifier.weight(1f))
                        Text(text = "✓", color = Brand500, style = MaterialTheme.typography.labelMedium)
                    }
                }
                Spacer(Modifier.height(8.dp))
                addresses.forEach { a ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPickAddress(a) }
                            .padding(vertical = 11.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "📍", fontSize = 15.sp)
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = a.name + if (a.isDefault) "（默认）" else "",
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (a.address.isNotBlank()) {
                                Text(text = a.address, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        if (current?.label == a.name) {
                            Text(text = "✓", color = Brand500, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onManage) { Text("管理地址") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

private fun SearchAddressEntity.toCenter(): SearchCenter =
    SearchCenter(label = name, lat = latitude, lng = longitude, address = address)

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
