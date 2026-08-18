package com.shijian.app.data.repo

import com.shijian.app.api.AmapPoi
import com.shijian.app.api.ApiClient
import com.shijian.app.data.db.dao.FoodPoiDao
import com.shijian.app.data.db.entity.FoodPoiEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlin.math.cos

/** 美食仓库：本地缓存 + 高德多点位搜索；所有挂起方法保证不抛异常（失败写入 _error） */
class FoodRepository(private val dao: FoodPoiDao) {

    private val _results = MutableStateFlow<List<FoodPoiEntity>>(emptyList())
    val results: StateFlow<List<FoodPoiEntity>> = _results

    private val _searching = MutableStateFlow(false)
    val searching: StateFlow<Boolean> = _searching

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun observeActive(): Flow<List<FoodPoiEntity>> = dao.observeActive()
    fun observeFavorites(): Flow<List<FoodPoiEntity>> = dao.observeFavorites()
    fun observeBlacklisted(): Flow<List<FoodPoiEntity>> = dao.observeBlacklisted()
    fun observeAll(): Flow<List<FoodPoiEntity>> = dao.observeAll()
    fun searchLocalFlow(kw: String): Flow<List<FoodPoiEntity>> = dao.searchLocal(kw)

    suspend fun toggleFavorite(poi: FoodPoiEntity) = runCatching {
        dao.upsert(poi.copy(isFavorite = !poi.isFavorite))
    }

    suspend fun setBlacklisted(poi: FoodPoiEntity, blacklisted: Boolean) = runCatching {
        dao.upsert(poi.copy(isBlacklisted = blacklisted))
    }

    suspend fun clearCache() = runCatching {
        dao.clearAll()
        _results.value = emptyList()
    }

    /** 随机推荐：优先收藏，其次未拉黑缓存；任何异常不向上抛（返回 null） */
    suspend fun randomPick(): FoodPoiEntity? = withContext(Dispatchers.IO) {
        runCatching { dao.randomFavorite() ?: dao.randomActive() }.getOrNull()
    }

    /** 周边搜索。pagesPerPoint：每个点位最多翻几页；multiPoint=true 时会按半径拆多个子点。 */
    suspend fun searchAround(
        amapKey: String,
        lat: Double,
        lng: Double,
        radiusKm: Int,
        keywords: String?,
        foodType: String?,
        multiPoint: Boolean,
        pagesPerPoint: Int = 2
    ): Boolean = withContext(Dispatchers.IO) {
        _searching.value = true
        _error.value = null
        try {
            val centerKey = "${lng},${lat}|${radiusKm}|${keywords ?: ""}|${foodType ?: ""}"
            val freshBefore = System.currentTimeMillis() - CACHE_TTL
            val cachedCount = runCatching { dao.cachedCount(centerKey, freshBefore) }.getOrDefault(0)
            if (cachedCount > 0) {
                val cached = dao.observeCached(centerKey, freshBefore).firstOrNull()
                if (!cached.isNullOrEmpty()) {
                    _results.value = cached.filter { p -> !p.isBlacklisted }
                    return@withContext false
                }
            }

            val types = FOOD_TYPES[foodType] ?: FOOD_TYPES["全部"] ?: "050000"
            val raw = mutableListOf<AmapPoi>()
            val seen = HashSet<String>()
            val pagesLimit = pagesPerPoint.coerceAtLeast(1)
            var hitQpsLimit = false

            if (multiPoint) {
                val n = when {
                    radiusKm <= 2 -> 2
                    radiusKm <= 5 -> 3
                    else -> 4
                }
                val points = runCatching { samplePoints(lat, lng, radiusKm, n) }.getOrDefault(listOf(lat to lng))
                var calls = 0
                val maxCalls = (n * pagesLimit).coerceAtMost(18)
                pointsLoop@
                for ((plat, plng) in points) {
                    var page = 1
                    while (page <= pagesLimit && calls < maxCalls) {
                        val resp = ApiClient.amap.around(
                            key = amapKey,
                            location = "$plng,$plat",
                            radius = radiusKm * 1000,
                            keywords = keywords,
                            types = types,
                            page = page
                        )
                        calls++
                        if (resp.status != "1") {
                            val info = resp.info.ifBlank { "高德返回错误：${resp.status}" }
                            _error.value = info
                            if (info.contains("CUQPS", ignoreCase = true)) hitQpsLimit = true
                            break@pointsLoop
                        }
                        if (resp.pois.isEmpty()) break
                        resp.pois.forEach { p -> if (seen.add(p.id)) raw.add(p) }
                        page++
                    }
                }
            } else {
                var page = 1
                while (page <= pagesLimit) {
                    val resp = ApiClient.amap.around(
                        key = amapKey,
                        location = "$lng,$lat",
                        radius = radiusKm * 1000,
                        keywords = keywords,
                        types = types,
                        page = page
                    )
                    if (resp.status != "1") {
                        val info = resp.info.ifBlank { "高德返回错误：${resp.status}" }
                        _error.value = info
                        if (info.contains("CUQPS", ignoreCase = true)) hitQpsLimit = true
                        break
                    }
                    if (resp.pois.isEmpty()) break
                    resp.pois.forEach { p -> if (seen.add(p.id)) raw.add(p) }
                    page++
                }
            }

            val now = System.currentTimeMillis()
            val entities = raw.mapNotNull { it.toEntityOrNull(centerKey, now) }
            if (entities.isNotEmpty()) {
                runCatching { dao.upsertAll(entities) }
            }
            _results.value = entities.filter { !it.isBlacklisted }
            if (hitQpsLimit && entities.isNotEmpty()) {
                _error.value = null
            }
            entities.isNotEmpty() || !hitQpsLimit
        } catch (e: Exception) {
            _error.value = "搜索失败：${e.message ?: "网络或 Key 异常"}"
            false
        } finally {
            _searching.value = false
        }
    }

    suspend fun searchByKeyword(amapKey: String, keyword: String, city: String?, pages: Int = 1) {
        withContext(Dispatchers.IO) {
            _searching.value = true
            _error.value = null
            try {
                if (keyword.isBlank()) { _results.value = emptyList(); return@withContext }
                val centerKey = "kw:$keyword|${city ?: ""}"
                val freshBefore = System.currentTimeMillis() - CACHE_TTL
                if (runCatching { dao.cachedCount(centerKey, freshBefore) }.getOrDefault(0) > 0) {
                    val cached = dao.observeCached(centerKey, freshBefore).firstOrNull()
                    if (!cached.isNullOrEmpty()) {
                        _results.value = cached.filter { p -> !p.isBlacklisted }
                        return@withContext
                    }
                }
                val raw = mutableListOf<AmapPoi>()
                val seen = HashSet<String>()
                var page = 1
                val pagesLimit = pages.coerceAtLeast(1)
                var hitQps = false
                while (page <= pagesLimit) {
                    val resp = ApiClient.amap.text(amapKey, keyword, city, page = page)
                    if (resp.status != "1") {
                        val info = resp.info.ifBlank { "高德返回错误：${resp.status}" }
                        _error.value = info
                        if (info.contains("CUQPS", ignoreCase = true)) hitQps = true
                        break
                    }
                    if (resp.pois.isEmpty()) break
                    resp.pois.forEach { p -> if (seen.add(p.id)) raw.add(p) }
                    page++
                }
                val now = System.currentTimeMillis()
                val entities = raw.mapNotNull { it.toEntityOrNull(centerKey, now) }
                if (entities.isNotEmpty()) runCatching { dao.upsertAll(entities) }
                _results.value = entities.filter { !it.isBlacklisted }
                if (hitQps && entities.isNotEmpty()) _error.value = null
            } catch (e: Exception) {
                _error.value = "搜索失败：${e.message ?: "网络或 Key 异常"}"
            } finally {
                _searching.value = false
            }
        }
    }

    suspend fun searchByAddress(
        amapKey: String,
        address: String,
        city: String?,
        radiusKm: Int,
        multiPoint: Boolean,
        pagesPerPoint: Int = 1
    ) {
        withContext(Dispatchers.IO) {
            _searching.value = true
            _error.value = null
            try {
                val resp = runCatching { ApiClient.amap.geocode(amapKey, address, city) }
                    .getOrElse { throw IllegalStateException("地址解析失败：${it.message}") }
                val loc = resp.geocodes.firstOrNull()?.location
                    ?: throw IllegalStateException("未解析到地址坐标")
                val parts = loc.split(",")
                if (parts.size != 2) throw IllegalStateException("坐标格式异常")
                searchAround(amapKey, parts[1].toDouble(), parts[0].toDouble(), radiusKm, null, null, multiPoint, pagesPerPoint)
            } catch (e: Exception) {
                _error.value = "地址搜索失败：${e.message ?: "地址或 Key 异常"}"
                _searching.value = false
            }
        }
    }

    private fun samplePoints(lat: Double, lng: Double, radiusKm: Int, n: Int): List<Pair<Double, Double>> {
        val kmPerDegLat = 111.32
        val kmPerDegLng = 111.32 * cos(Math.toRadians(lat)).coerceAtLeast(0.01)
        val spacing = 2.0 * radiusKm.coerceAtLeast(1) / (n - 1).coerceAtLeast(1)
        val points = mutableListOf<Pair<Double, Double>>()
        for (i in 0 until n) {
            for (j in 0 until n) {
                val dLat = (i - (n - 1) / 2.0) * spacing
                val dLng = (j - (n - 1) / 2.0) * spacing
                points.add(Pair(lat + dLat / kmPerDegLat, lng + dLng / kmPerDegLng))
            }
        }
        return points
    }

    /** toEntity 容错版：任何字段为空/格式错误均返回 null，不丢入结果集。 */
    private fun AmapPoi.toEntityOrNull(centerKey: String, now: Long): FoodPoiEntity? = runCatching {
        if (id.isBlank() && name.isBlank()) return@runCatching null
        val parts = location.split(",")
        FoodPoiEntity(
            id = id.ifBlank { "${name}_${address}_${location}" },
            name = name,
            type = when {
                type.contains(';') -> type.substringAfter(';')
                type.isNotBlank() -> type
                else -> "美食"
            },
            address = address,
            longitude = parts.getOrNull(0)?.toDoubleOrNull() ?: 0.0,
            latitude = parts.getOrNull(1)?.toDoubleOrNull() ?: 0.0,
            distance = distance ?: 0,
            rating = rating,
            cost = cost,
            photos = photos?.joinToString(",") { it.url } ?: "",
            source = "amap",
            searchCenter = centerKey,
            cachedAt = now
        )
    }.getOrNull()

    companion object {
        private const val CACHE_TTL = 30L * 24 * 3600 * 1000 // 30 天
        private const val MAX_PAGES_PER_POINT = 8
        private const val MAX_TOTAL_CALLS = 120

        val FOOD_TYPES = mapOf(
            "全部" to "050000",
            "火锅" to "050301",
            "烧烤" to "050310",
            "甜品" to "050311",
            "快餐" to "050104",
            "日料" to "050304",
            "西餐" to "050309",
            "奶茶" to "050203",
            "小吃" to "050307",
            "面食" to "050201",
            "咖啡" to "050205"
        )

        val FOOD_CHIPS = listOf(
            "全部", "火锅", "烧烤", "面食", "甜品", "快餐", "日料", "西餐", "奶茶", "小吃"
        )

        val SORT_OPTIONS = listOf("综合", "距离最近", "评分最高")
    }
}
