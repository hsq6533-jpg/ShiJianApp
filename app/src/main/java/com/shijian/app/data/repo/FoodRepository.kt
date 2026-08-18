package com.shijian.app.data.repo

import com.shijian.app.api.AmapPoi
import com.shijian.app.api.ApiClient
import com.shijian.app.data.db.dao.FoodPoiDao
import com.shijian.app.data.db.entity.FoodPoiEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlin.math.cos

/** 美食仓库：本地缓存 + 高德多点位搜索 */
class FoodRepository(private val dao: FoodPoiDao) {

    private val _results = MutableStateFlow<List<FoodPoiEntity>>(emptyList())
    /** 当前搜索结果（已过滤黑名单） */
    val results: StateFlow<List<FoodPoiEntity>> = _results

    /** 是否正在搜索 */
    private val _searching = MutableStateFlow(false)
    val searching: StateFlow<Boolean> = _searching

    /** 最后一次搜索失败信息 */
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun observeActive(): Flow<List<FoodPoiEntity>> = dao.observeActive()

    fun observeFavorites(): Flow<List<FoodPoiEntity>> = dao.observeFavorites()

    fun observeBlacklisted(): Flow<List<FoodPoiEntity>> = dao.observeBlacklisted()

    fun observeAll(): Flow<List<FoodPoiEntity>> = dao.observeAll()

    fun searchLocalFlow(kw: String): Flow<List<FoodPoiEntity>> = dao.searchLocal(kw)

    suspend fun toggleFavorite(poi: FoodPoiEntity) = dao.upsert(poi.copy(isFavorite = !poi.isFavorite))

    suspend fun setBlacklisted(poi: FoodPoiEntity, blacklisted: Boolean) =
        dao.upsert(poi.copy(isBlacklisted = blacklisted))

    suspend fun clearCache() {
        dao.clearAll()
        _results.value = emptyList()
    }

    /** 随机推荐：优先收藏，其次未拉黑缓存 */
    suspend fun randomPick(): FoodPoiEntity? =
        dao.randomFavorite() ?: dao.randomActive()

    /**
     * 周边搜索（7.1）
     * @return 命中缓存返回 false，走网络并落库返回 true；失败抛异常
     */
    suspend fun searchAround(
        amapKey: String,
        lat: Double,
        lng: Double,
        radiusKm: Int,
        keywords: String?,
        foodType: String?,
        multiPoint: Boolean
    ): Boolean {
        _searching.value = true
        _error.value = null
        try {
            val centerKey = "${lng},${lat}|${radiusKm}"
            val freshBefore = System.currentTimeMillis() - CACHE_TTL
            val cachedCount = dao.cachedCount(centerKey, freshBefore)
            if (cachedCount > 0) {
                val cached = dao.observeCached(centerKey, freshBefore)
                cached.firstOrNull()?.let {
                    _results.value = it.filter { p -> !p.isBlacklisted }
                }
                return false
            }

            val types = foodType?.let { FOOD_TYPES[it] } ?: FOOD_TYPES[foodType] ?: "050000"
            val raw = mutableListOf<AmapPoi>()
            val seen = HashSet<String>()

            if (multiPoint) {
                val n = when {
                    radiusKm <= 2 -> 3
                    radiusKm <= 5 -> 5
                    else -> 7
                }
                val points = samplePoints(lat, lng, radiusKm, n)
                var calls = 0
                for ((plat, plng) in points) {
                    var page = 1
                    while (page <= MAX_PAGES_PER_POINT && calls < MAX_TOTAL_CALLS) {
                        val resp = ApiClient.amap.around(
                            key = amapKey,
                            location = "$plng,$plat",
                            radius = radiusKm * 1000,
                            keywords = keywords,
                            types = types,
                            page = page
                        )
                        calls++
                        if (resp.status != "1" || resp.pois.isEmpty()) break
                        resp.pois.forEach { p -> if (seen.add(p.id)) raw.add(p) }
                        page++
                    }
                }
            } else {
                var page = 1
                while (page <= MAX_PAGES_PER_POINT) {
                    val resp = ApiClient.amap.around(
                        key = amapKey,
                        location = "$lng,$lat",
                        radius = radiusKm * 1000,
                        keywords = keywords,
                        types = types,
                        page = page
                    )
                    if (resp.status != "1" || resp.pois.isEmpty()) break
                    resp.pois.forEach { p -> if (seen.add(p.id)) raw.add(p) }
                    page++
                }
            }

            val now = System.currentTimeMillis()
            val entities = raw.map { it.toEntity(centerKey, now) }
            dao.upsertAll(entities)
            _results.value = entities.filter { !it.isBlacklisted }
            return true
        } finally {
            _searching.value = false
        }
    }

    /** 关键词搜索（place/text） */
    suspend fun searchByKeyword(amapKey: String, keyword: String, city: String?) {
        _searching.value = true
        _error.value = null
        try {
            val centerKey = "kw:$keyword|${city ?: ""}"
            val freshBefore = System.currentTimeMillis() - CACHE_TTL
            if (dao.cachedCount(centerKey, freshBefore) > 0) {
                val cached = dao.observeCached(centerKey, freshBefore)
                cached.firstOrNull()?.let {
                    _results.value = it.filter { p -> !p.isBlacklisted }
                }
                return
            }
            val raw = mutableListOf<AmapPoi>()
            val seen = HashSet<String>()
            var page = 1
            while (page <= MAX_PAGES_PER_POINT) {
                val resp = ApiClient.amap.text(amapKey, keyword, city, page = page)
                if (resp.status != "1" || resp.pois.isEmpty()) break
                resp.pois.forEach { p -> if (seen.add(p.id)) raw.add(p) }
                page++
            }
            val now = System.currentTimeMillis()
            val entities = raw.map { it.toEntity(centerKey, now) }
            dao.upsertAll(entities)
            _results.value = entities.filter { !it.isBlacklisted }
        } catch (e: Exception) {
            _error.value = "搜索失败，请检查 Key 或网络"
        } finally {
            _searching.value = false
        }
    }

    /** 按地址搜索：先地理编码，再周边搜索 */
    suspend fun searchByAddress(
        amapKey: String,
        address: String,
        city: String?,
        radiusKm: Int,
        multiPoint: Boolean
    ) {
        val resp = ApiClient.amap.geocode(amapKey, address, city)
        val loc = resp.geocodes.firstOrNull()?.location ?: throw IllegalStateException("地址解析失败")
        val parts = loc.split(",")
        if (parts.size != 2) throw IllegalStateException("地址解析失败")
        searchAround(amapKey, parts[1].toDouble(), parts[0].toDouble(), radiusKm, null, null, multiPoint)
    }

    private fun samplePoints(lat: Double, lng: Double, radiusKm: Int, n: Int): List<Pair<Double, Double>> {
        val kmPerDegLat = 111.32
        val kmPerDegLng = 111.32 * cos(Math.toRadians(lat)).coerceAtLeast(0.01)
        val spacing = 2.0 * radiusKm / (n - 1)
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

    private fun AmapPoi.toEntity(centerKey: String, now: Long): FoodPoiEntity {
        val parts = location.split(",")
        return FoodPoiEntity(
            id = id.ifBlank { name + location },
            name = name,
            type = type.substringAfter(';').ifBlank { type },
            address = address,
            longitude = parts.getOrNull(0)?.toDoubleOrNull() ?: 0.0,
            latitude = parts.getOrNull(1)?.toDoubleOrNull() ?: 0.0,
            distance = distance ?: 0,
            rating = rating,
            cost = cost,
            photos = photos?.joinToString(",", transform = { it.url }) ?: "",
            source = "amap",
            searchCenter = centerKey,
            cachedAt = now
        )
    }

    companion object {
        private const val CACHE_TTL = 30L * 24 * 3600 * 1000 // 30 天
        private const val MAX_PAGES_PER_POINT = 8
        private const val MAX_TOTAL_CALLS = 120

        /** 分类 chips → 高德 types（050000 为餐饮服务大类） */
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
