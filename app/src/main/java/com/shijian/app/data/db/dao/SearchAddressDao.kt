package com.shijian.app.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.shijian.app.data.db.entity.SearchAddressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchAddressDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(a: SearchAddressEntity): Long

    @Update
    suspend fun update(a: SearchAddressEntity)

    @Delete
    suspend fun delete(a: SearchAddressEntity)

    @Query("SELECT * FROM search_addresses ORDER BY isDefault DESC, sortOrder ASC, id ASC")
    fun observeAll(): Flow<List<SearchAddressEntity>>

    @Query("UPDATE search_addresses SET isDefault = 0")
    suspend fun clearAllDefault()

    @Query("SELECT * FROM search_addresses WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefault(): SearchAddressEntity?

    @Query("DELETE FROM search_addresses")
    suspend fun clearAll()
}
