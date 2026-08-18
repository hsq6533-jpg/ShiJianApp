package com.shijian.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.shijian.app.data.db.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<CategoryEntity>)

    @Query("SELECT * FROM categories WHERE type = :type ORDER BY sortOrder ASC")
    fun observeByType(type: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY type ASC, sortOrder ASC")
    suspend fun getAll(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): CategoryEntity?

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int
}
