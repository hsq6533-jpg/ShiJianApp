package com.shijian.app.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.shijian.app.data.db.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(t: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<TransactionEntity>)

    @Update
    suspend fun update(t: TransactionEntity)

    @Delete
    suspend fun delete(t: TransactionEntity)

    @Query("DELETE FROM transactions")
    suspend fun clearAll()

    @Query("SELECT * FROM transactions WHERE id = :id")
    fun observeById(id: Long): Flow<TransactionEntity?>

    @Query("SELECT * FROM transactions")
    fun observeAll(): Flow<List<TransactionEntity>>

    /** 区间查询（含首尾），按日期/时间倒序 */
    @Query("SELECT * FROM transactions WHERE date >= :start AND date <= :end ORDER BY date DESC, time DESC")
    fun observeRange(start: String, end: String): Flow<List<TransactionEntity>>

    /** 指定日期 */
    @Query("SELECT * FROM transactions WHERE date = :date ORDER BY time DESC")
    fun observeByDate(date: String): Flow<List<TransactionEntity>>

    /** 当日奶茶杯数 */
    @Query("SELECT COUNT(*) FROM transactions WHERE date = :date AND isMilkTea = 1")
    suspend fun milkTeaCountOn(date: String): Int

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun countAll(): Int

    @Query("SELECT COUNT(DISTINCT date) FROM transactions")
    suspend fun countDays(): Int

    @Query("SELECT COUNT(*) FROM transactions")
    fun observeCount(): Flow<Int>
}
