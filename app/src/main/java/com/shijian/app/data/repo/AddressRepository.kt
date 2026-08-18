package com.shijian.app.data.repo

import com.shijian.app.data.db.dao.SearchAddressDao
import com.shijian.app.data.db.entity.SearchAddressEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

/** 搜索地址仓库；所有挂起方法不抛异常。 */
class AddressRepository(private val dao: SearchAddressDao) {

    fun observeAll(): Flow<List<SearchAddressEntity>> = dao.observeAll()

    suspend fun add(name: String, address: String, lng: Double?, lat: Double?) = runCatching {
        dao.insert(SearchAddressEntity(name = name, address = address, longitude = lng, latitude = lat))
    }

    suspend fun update(a: SearchAddressEntity) = runCatching { dao.update(a) }

    suspend fun delete(a: SearchAddressEntity) = runCatching { dao.delete(a) }

    suspend fun setDefault(a: SearchAddressEntity) = runCatching {
        val current = dao.observeAll().firstOrNull()?.firstOrNull { it.isDefault }
        if (current != null && current.id != a.id) runCatching { dao.update(current.copy(isDefault = false)) }
        dao.update(a.copy(isDefault = true))
    }

    suspend fun getDefault(): SearchAddressEntity? = runCatching { dao.getDefault() }.getOrNull()

    suspend fun clearAll() = runCatching { dao.clearAll() }
}
