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
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.ui.text.input.ImeAction
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

private val ADDRESS_KEYWORDS = listOf(
    "路", "街", "道", "巷", "村", "镇", "区", "号",
    "大厦", "广场", "中心", "小区", "公寓", "酒店", "公园", "桥", "门", "大道"
)

private fun isAddressQuery(q: String): Boolean {
    val s = q.trim()
    if (s.length < 3) return false
    return ADDRESS_KEYWORDS.any { s.contains(it) }
}

private data class SearchCenter(
    val label: String,
    val lat: Double? = null,
    val lng: Double? = null,
    val address: String? = null,
)

private fun friendlyError(raw: String?): String = when {
    raw.isNullOrBlank() -> "搜索失败，请重试"
    raw.contains("CUQPS", ignoreCase = true) -> "搜索过于频繁，请稍后再试"
    raw.contains("DAILY_QUERY", ignoreCase = true) -> "今日搜索额度已用完，明天再试"
    raw.contains("INVALID_USER_KEY", ignoreCase = true) || raw.contains("INVALID_KEY", ignoreCase = true) -> "高德 Key 无效，请检查"
    raw.contains("USERKEY_PLAT", ignoreCase = true) -> "高德 Key 绑定域名不匹配，请更换 Web 服务 Key"
    raw.contains("OUT_OF_SERVICE", ignoreCase = true) -> "当前区域暂无数据"
    else -> raw
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FoodScreen(
    container: AppContainer,
    nav: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val settings by remember {
        container.settingsRepo.settings.catch { emit(AppSettings()) }
    }.collectAsStateWithLifecycle(initialValue = AppSettings())
    val addresses by remember {
        container.addressRepo.observeAll().catch { emit(emptyList()) }
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    val results by remember {
        container.foodRepo.results.catch { emit(emptyList()) }
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    val searching by remember {
        container.foodRepo.searching.catch { emit(false) }
    }.collectAsStateWithLifecycle(initialValue = false)
    val error by remember {
        container.foodRepo.error.catch { emit(null) }
    }.collectAsStateWithLifecycle(initialValue = null)

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

    val filterLocal: (List<FoodPoiEntity>, String, String, String) -> List<FoodPoiEntity> = {
        list, keyword, cat, srt ->
        val base = list.filter { !it.isBlacklisted }
        val kwTrimed = keyword.trim()
        val afterKw = if (kwTrimed.isBlank()) base else base.filter {
            it.name.contains(kwTrimed) || it.type.contains(kwTrimed) || it.address.contains(kwTrimed)
        }
        val afterCat = if (cat == "全部") afterKw else afterKw.filter { it.type.contains(cat) || it.name.contains(cat) }
        when (srt) {
            "距离最近" -> afterCat.sortedBy { it.distance }
            "评分最高" -> afterCat.sortedByDescending { it.rating ?: -1f }
            else -> afterCat
        }
    }

    val pick: () -> Unit = {
        picking = true
        scope.launch {
            runCatching {
                val base = container.foodRepo.results.value.filter { !it.isBlacklisted }.ifEmpty {
                    runCatching {
                        container.database.foodPoiDao().observeActive().let { flow ->
                            kotlinx.coroutines.flow.firstOrNull(flow)
                        }
                    }.getOrNull() ?: emptyList()
                }
                if (base.isEmpty()) { picked = null; return@runCatching }
                val preferred = base.filter { poi ->
                    val t = poi.type + poi.name
                    !(t.contains("奶茶") || t.contains("甜品") || t.contains("咖啡") ||
                        t.contains("饮品") || t.contains("蛋糕") || t.contains("面包") ||
                        t.contains("冰淇淋") || t.contains("茶艺") || t.contains("果饮"))
                }
                picked = (preferred.ifEmpty { base }).randomOrNull()
            }
            picking = false
        }
    }

    var locatedOnce by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (locatedOnce) return@LaunchedEffect
        locatedOnce = true
        var c: SearchCenter? = null
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            pickingLocation = true
            val loc = runCatching { LocationUtils.getCurrentLocation(context) }.getOrNull()
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

    val toast: (String) -> Unit = { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }

    var useGps: () -> Unit = {}
    val locationPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) useGps() else toast("未授予定位权限，请在设置中允许后使用定位")
    }
    useGps = {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            locationPermLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            scope.launch {
                pickingLocation = true
                val loc = LocationUtils.getCurrentLocation(context)
                pickingLocation = false
                if (loc != null) {
                    center = SearchCenter("我的定位", loc.latitude, loc.longitude)
                    toast("已更新定位")
                } else {
                    toast("定位失败，请检查定位是否开启")
                }
            }
        }
    }

    val doSearch: () -> Unit = doSearch@ {
        val key = container.securePrefs.getAmapKey()
        if (key.isNullOrBlank()) { showKeySheet = true; toast("请先配置高德 Key"); return@doSearch }
        hasSearched = true
        scope.launch {
            runCatching {
                val q = kw.trim()
                val c = center
                when {
                    isAddressQuery(q) -> container.foodRepo.searchByAddress(key, q, null, settings.searchRadiusKm, multiPoint = false, pagesPerPoint = 1)
                    c?.lat != null && c.lng != null -> container.foodRepo.searchAround(key, c.lat!!, c.lng!!, settings.searchRadiusKm, keywords = q.ifBlank { null }, foodType = null, multiPoint = false, pagesPerPoint = 2)
                    else -> container.foodRepo.searchByKeyword(key, q, null, pages = 1)
                }
            }.onFailure { toast("搜索失败，请检查 Key 或网络") }
        }
    }

    val displayed = remember(results, kw, category, sort) { filterLocal(results, kw, category, sort) }
    val categoryCounts = remember(results) {
        val all = results.filter { !it.isBlacklisted }
        val counts = linkedMapOf<String, Int>()
        counts["全部"] = all.size
        FoodRepository.FOOD_CHIPS.forEach { chip ->
            if (chip == "全部") return@forEach
            counts[chip] = all.count { it.type.contains(chip) || it.name.contains(chip) }
        }
        counts
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        TabTopBar(
            title = "美食",
            actions = {
                Row {
                    IconButton(onClick = { showCenterPicker = true }) {
                        Icon(Icons.Filled.Tune, contentDescription = "搜索中心", tint = MaterialTheme.colorScheme.onBackground)
                    }
                    Box {
                        IconButton(onClick = { showKeySheet = true; keyInput = amapKey.orEmpty() }) {
                            Icon(Icons.Filled.LocationOn, contentDescription = "美食设置", tint = MaterialTheme.colorScheme.onBackground)
                        }
                        if (amapKey.isNullOrBlank()) {
                            Box(modifier = Modifier.size(9.dp).background(Danger500, CircleShape).align(Alignment.TopEnd))
                        }
                    }
                }
            }
        )

        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
        ) {
            RandomHeroCard(
                picked = picked, picking = picking, center = center, radiusKm = settings.searchRadiusKm,
                onRandom = pick, onSearchCenter = { showCenterPicker = true }
            ) { p -> AmapIntents.openNavigation(context, p.latitude, p.longitude, p.name) }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = kw, onValueChange = { kw = it },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                placeholder = {
                    Text("搜地址、美食名或关键词（留空=搜全部）", style = MaterialTheme.typography.bodyMedium)
                },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                },
                trailingIcon = {
                    Box(
                        modifier = Modifier.height(36.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(999.dp)).clickable(onClick = doSearch).padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searching) "搜索中" else "搜索",
                            color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                singleLine = true, maxLines = 1,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { doSearch() }),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Brand500)
            )

            Spacer(Modifier.height(10.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(FoodRepository.FOOD_CHIPS) { chip ->
                    val sel = chip == category
                    val count = categoryCounts[chip] ?: 0
                    val disabled = count == 0 && chip != "全部"
                    Box(
                        modifier = Modifier
                            .background(
                                when {
                                    sel -> MaterialTheme.colorScheme.primary
                                    disabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    else -> MaterialTheme.colorScheme.surface
                                }, RoundedCornerShape(999.dp)
                            )
                            .clickable(enabled = !disabled) { category = chip }
                            .padding(horizontal = 13.dp, vertical = 7.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = chip, style = MaterialTheme.typography.labelMedium,
                                color = when {
                                    sel -> Color.White
                                    disabled -> TextSecondary.copy(alpha = 0.6f)
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                            if (results.isNotEmpty() && count > 0) {
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = count.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (sel) Color.White.copy(alpha = 0.9f) else TextSecondary,
                                    fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SORT_OPTIONS.forEach { s ->
                    val sel = s == sort
                    Box(
                        modifier = Modifier
                            .background(if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface, RoundedCornerShape(999.dp))
                            .clickable { sort = s }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(text = s, style = MaterialTheme.typography.labelSmall, color = if (sel) Color.White else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (kw.isNotBlank() && hasSearched) "搜索结果" else "美食",
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = when {
                        searching -> "搜索中"
                        error != null -> friendlyError(error)
                        displayed.isNotEmpty() -> "共 ${displayed.size} 家 本地缓存"
                        !hasSearched -> if (amapKey.isNullOrBlank()) "先配置高德 Key，再点搜索" else "点击「搜索」抓取附近美食"
                        else -> "暂无结果，换关键词或调整范围"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (error != null) Danger500 else TextSecondary,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(10.dp))

            if (displayed.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    displayed.forEach { poi ->
                        FoodPoiCard(
                            poi = poi,
                            onFav = { scope.launch { runCatching { container.foodRepo.toggleFavorite(poi) } } },
                            onMap = { AmapIntents.openNavigation(context, poi.latitude, poi.longitude, poi.name) },
                            onBlock = { blacklistTarget = poi },
                            onAddressClick = { AmapIntents.openNavigation(context, poi.latitude, poi.longitude, poi.name) },
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
                val hint = when {
                    error != null -> friendlyError(error)
                    !hasSearched -> "点右上角「搜索」加载附近美食"
                    else -> "换个关键词或在设置里扩大搜索范围"
                }
                EmptyState(emoji = "X", title = if (amapKey.isNullOrBlank()) "尚未配置高德 Key" else "暂无美食", subtitle = hint)
            }

            Spacer(Modifier.height(18.dp))
        }
    }

    if (showCenterPicker) {
        SearchCenterDialog(
            addresses = addresses, current = center, locating = pickingLocation, radiusKm = settings.searchRadiusKm,
            onDismiss = { showCenterPicker = false },
            onGps = { useGps(); showCenterPicker = false },
            onPickAddress = { a ->
                center = a.toCenter(); showCenterPicker = false; toast("已切换到「${a.name}」")
            },
            onManage = { showCenterPicker = false; nav(Routes.ADDRESS_MANAGE) },
            onSettings = { showCenterPicker = false; nav(Routes.FOOD_SETTINGS) }
        )
    }

    if (showKeySheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(onDismissRequest = { showKeySheet = false }, sheetState = sheetState) {
            Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
                Text(text = "美食设置", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(text = "去 https://lbs.amap.com 申请 Web 服务 Key", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = keyInput, onValueChange = { keyInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("高德 Web 服务 API Key") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Brand500)
                )
                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().height(48.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(999.dp)).clickable {
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

    blacklistTarget?.let { poi ->
        AlertDialog(
            onDismissRequest = { blacklistTarget = null },
            title = { Text("拉黑该店铺？") },
            text = { Text("「${poi.name}」将不再出现在搜索和随机推荐中。") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { runCatching { container.foodRepo.setBlacklisted(poi, true) }
                    blacklistTarget = null
                }) { Text("拉黑", color = Danger500) }
            },
            dismissButton = { TextButton(onClick = { blacklistTarget = null }) { Text("取消") } }
        )
    }
}

@Composable
private fun RandomHeroCard(
    picked: FoodPoiEntity?,
    picking: Boolean,
    center: SearchCenter?,
    radiusKm: Int,
    onRandom: () -> Unit,
    onSearchCenter: () -> Unit,
    onGo: (FoodPoiEntity) -> Unit
) {
    SjCard(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Y 今天吃什么", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(999.dp))
                    .clickable { onSearchCenter() }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Brand500, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = when {
                            center == null -> "选择搜索中心"
                            center.label == "我的定位" -> "当前定位 A ${radiusKm}km"
                            else -> "${center.label} A ${radiusKm}km"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = Brand500,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier.size(36.dp).background(MaterialTheme.colorScheme.primary, CircleShape).clickable { onRandom() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Shuffle, contentDescription = "a", tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }

        if (picking) {
            Spacer(Modifier.height(10.dp))
            Text("挑选中", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        } else if (picked != null) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp)).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(picked.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(2.dp))
                    Text("${picked.type} A ${FormatUtils.distance(picked.distance)} A ${FormatUtils.cost(picked.cost)}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                Box(
                    modifier = Modifier.background(Brand500, RoundedCornerShape(10.dp)).clickable { onGo(picked) }.padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Text("a", color = Color.White, style = MaterialTheme.typography.labelSmall)
                }
            }
        } else {
            Spacer(Modifier.height(10.dp))
            Text("还没结果？点「搜索」先加载附近美食，之后再随机", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}

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
                modifier = Modifier.size(40.dp).background(Brand500.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) { Text(foodEmoji(poi.type), fontSize = 20.sp) }
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
                        Text("= ${FormatUtils.rating(it)}", style = MaterialTheme.typography.labelSmall, color = Orange500)
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text("${poi.type} A ${FormatUtils.distance(poi.distance)} A ${FormatUtils.cost(poi.cost)}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }

        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .combinedClickable(onClick = onAddressClick, onLongClick = onAddressLongPress)
                .padding(horizontal = 10.dp, vertical = 7.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocationOn, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(5.dp))
                Text(
                    text = poi.address.ifBlank { "暂无详细地址" },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionPill(
                icon = {
                    Icon(
                        if (poi.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        tint = if (poi.isFavorite) Danger500 else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(15.dp)
                    )
                },
                text = "a", tint = if (poi.isFavorite) Danger500 else null, onClick = onFav
            )
            ActionPill(
                icon = { Icon(Icons.Filled.Map, contentDescription = null, tint = Brand500, modifier = Modifier.size(15.dp)) },
                text = "b", tint = Brand500, onClick = onMap
            )
            ActionPill(
                icon = { Icon(Icons.Filled.Block, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(15.dp)) },
                text = "c", tint = TextSecondary, onClick = onBlock
            )
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
            .padding(vertical = 7.dp),
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

@Composable
private fun SearchCenterDialog(
    addresses: List<SearchAddressEntity>,
    current: SearchCenter?,
    locating: Boolean,
    radiusKm: Int,
    onDismiss: () -> Unit,
    onGps: () -> Unit,
    onPickAddress: (SearchAddressEntity) -> Unit,
    onManage: () -> Unit,
    onSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("搜索中心 A ${radiusKm}km") },
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
                        text = if (locating) "正在定位" else "当前定位",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Brand500
                    )
                    if (current?.label == "我的定位") {
                        Spacer(Modifier.weight(1f))
                        Text("a", color = Brand500, style = MaterialTheme.typography.labelMedium)
                    }
                }
                Spacer(Modifier.height(8.dp))
                addresses.forEach { a ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPickAddress(a) }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "A", fontSize = 14.sp)
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = a.name + if (a.isDefault) "B" else "",
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (a.address.isNotBlank()) {
                                Text(a.address, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        if (current?.label == a.name) {
                            Text("a", color = Brand500, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().clickable { onSettings() }.padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("调整搜索范围（1A10km）", color = Brand500, style = MaterialTheme.typography.labelMedium)
                }
            }
        },
        confirmButton = { TextButton(onClick = onManage) { Text("管理地址") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}

private fun SearchAddressEntity.toCenter(): SearchCenter =
    SearchCenter(label = name, lat = latitude, lng = longitude, address = address)

private fun foodEmoji(type: String): String = when {
    type.contains("火锅") -> "C"
    type.contains("烧烤") || type.contains("烤肉") -> "D"
    type.contains("面") || type.contains("粉") || type.contains("馄饨") || type.contains("饺子") || type.contains("包子") -> "E"
    type.contains("甜品") || type.contains("蛋糕") || type.contains("面包") || type.contains("烘焙") || type.contains("冰淇淋") -> "F"
    type.contains("奶茶") || type.contains("咖啡") || type.contains("饮品") -> "G"
    type.contains("快餐") || type.contains("汉堡") || type.contains("披萨") -> "H"
    type.contains("日") || type.contains("寿司") || type.contains("韩") -> "I"
    type.contains("西餐") -> "J"
    type.contains("小吃") || type.contains("粤") -> "K"
    type.contains("海鲜") || type.contains("水产") -> "L"
    type.contains("水果") || type.contains("鲜果") -> "M"
    else -> "N"
}
