package com.shijian.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.shijian.app.data.db.entity.FoodPoiEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodPoiDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(list: List<FoodPoiEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(poi: FoodPoiEntity)

    @Query("SELECT * FROM food_pois WHERE isFavorite = 1 ORDER BY name ASC")
    fun observeFavorites(): Flow<List<FoodPoiEntity>>

    @Query("SELECT * FROM food_pois WHERE isBlacklisted = 1 ORDER BY name ASC")
    fun observeBlacklisted(): Flow<List<FoodPoiEntity>>

    @Query("SELECT * FROM food_pois")
    fun observeAll(): Flow<List<FoodPoiEntity>>

    @Query("SELECT * FROM food_pois WHERE isBlacklisted = 0 ORDER BY distance ASC")
    fun observeActive(): Flow<List<FoodPoiEntity>>

    /** 本地关键词搜索（缓存优先） */
    @Query("SELECT * FROM food_pois WHERE isBlacklisted = 0 AND (name LIKE '%' || :kw || '%' OR type LIKE '%' || :kw || '%' OR address LIKE '%' || :kw || '%') ORDER BY distance ASC")
    fun searchLocal(kw: String): Flow<List<FoodPoiEntity>>

    /** 按中心点 + 关键词/类型 本地筛选（用于全量缓存后的二次搜索） */
    @Query("SELECT * FROM food_pois WHERE searchCenter = :center AND cachedAt >= :freshBefore AND isBlacklisted = 0 " +
        "AND (:kw IS NULL OR name LIKE '%' || :kw || '%' OR type LIKE '%' || :kw || '%' OR address LIKE '%' || :kw || '%') " +
        "AND (:type IS NULL OR type LIKE '%' || :type || '%') " +
        "ORDER BY distance ASC")
    suspend fun searchLocalByCenter(center: String, freshBefore: Long, kw: String?, type: String?): List<FoodPoiEntity>

    @Query("SELECT * FROM food_pois WHERE searchCenter = :center AND cachedAt >= :freshBefore")
    fun observeCached(center: String, freshBefore: Long): Flow<List<FoodPoiEntity>>

    @Query("SELECT COUNT(*) FROM food_pois WHERE searchCenter = :center AND cachedAt >= :freshBefore")
    suspend fun cachedCount(center: String, freshBefore: Long): Int

    @Query("DELETE FROM food_pois")
    suspend fun clearAll()

    /** 清理过期缓存（保留收藏/黑名单标记） */
    @Query("DELETE FROM food_pois WHERE cachedAt < :before AND isFavorite = 0 AND isBlacklisted = 0")
    suspend fun clearExpired(before: Long)

    @Query("SELECT * FROM food_pois WHERE isFavorite = 1 AND isBlacklisted = 0 ORDER BY RANDOM() LIMIT 1")
    suspend fun randomFavorite(): FoodPoiEntity?

    @Query("SELECT * FROM food_pois WHERE isBlacklisted = 0 ORDER BY RANDOM() LIMIT 1")
    suspend fun randomActive(): FoodPoiEntity?

    @Query("SELECT COUNT(*) FROM food_pois")
    fun observeCount(): Flow<Int>
}
