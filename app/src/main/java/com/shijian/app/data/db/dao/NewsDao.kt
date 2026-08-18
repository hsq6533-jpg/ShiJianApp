package com.shijian.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.shijian.app.data.db.entity.NewsConfigEntity
import com.shijian.app.data.db.entity.NewsItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NewsDao {

    // ---- 新闻记录 ----
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<NewsItemEntity>)

    @Query("SELECT * FROM news_items ORDER BY publishedAt DESC")
    fun observeAll(): Flow<List<NewsItemEntity>>

    @Query("DELETE FROM news_items")
    suspend fun clearAll()

    /** 标记已读 */
    @Query("UPDATE news_items SET isRead = 1 WHERE id = :id")
    suspend fun markRead(id: String)

    // ---- 新闻配置（单行） ----
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveConfig(c: NewsConfigEntity)

    @Query("SELECT * FROM news_config WHERE id = 1")
    fun observeConfig(): Flow<NewsConfigEntity?>

    @Query("SELECT * FROM news_config WHERE id = 1")
    suspend fun getConfig(): NewsConfigEntity?
}
