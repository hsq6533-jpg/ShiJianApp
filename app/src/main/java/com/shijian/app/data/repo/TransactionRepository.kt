package com.shijian.app.data.repo

import com.shijian.app.data.db.dao.TransactionDao
import com.shijian.app.data.db.entity.TransactionEntity
import com.shijian.app.util.DateUtils
import kotlinx.coroutines.flow.Flow

/** 记账仓库 */
class TransactionRepository(private val dao: TransactionDao) {

    fun all(): Flow<List<TransactionEntity>> = dao.observeAll()

    fun month(year: Int, month: Int): Flow<List<TransactionEntity>> {
        val (s, e) = DateUtils.monthRange(year, month)
        return dao.observeRange(s, e)
    }

    fun weekRange(start: String, end: String): Flow<List<TransactionEntity>> =
        dao.observeRange(start, end)

    fun byDate(date: String): Flow<List<TransactionEntity>> = dao.observeByDate(date)

    fun byId(id: Long): Flow<TransactionEntity?> = dao.observeById(id)

    suspend fun insert(t: TransactionEntity): Long = dao.insert(t)

    suspend fun update(t: TransactionEntity) = dao.update(t)

    /** 标记报销状态（报销或撤销报销） */
    suspend fun setReimbursed(id: Long, reimbursed: Boolean) = dao.setReimbursed(id, reimbursed)

    suspend fun delete(t: TransactionEntity) = dao.delete(t)

    suspend fun clearAll() = dao.clearAll()

    suspend fun milkTeaCountOn(date: String): Int = dao.milkTeaCountOn(date)

    suspend fun countAll(): Int = dao.countAll()

    suspend fun countDays(): Int = dao.countDays()

    fun countFlow(): Flow<Int> = dao.observeCount()
}
