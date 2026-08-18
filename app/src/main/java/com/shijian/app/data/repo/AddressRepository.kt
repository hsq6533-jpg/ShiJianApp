package com.shijian.app.data.repo

import com.shijian.app.data.db.dao.SearchAddressDao
import com.shijian.app.data.db.entity.SearchAddressEntity
import kotlinx.coroutines.flow.Flow

/** 搜索地址仓库 */
class AddressRepository(private val dao: SearchAddressDao) {

    fun observeAll(): Flow<List<SearchAddressEntity>> = dao.observeAll()

    suspend fun add(name: String, address: String, lng: Double?, lat: Double?) {
        dao.insert(SearchAddressEntity(name = name, address = address, longitude = lng, latitude = lat))
    }

    suspend fun update(a: SearchAddressEntity) = dao.update(a)

    suspend fun delete(a: SearchAddressEntity) = dao.delete(a)

    suspend fun setDefault(a: SearchAddressEntity) {
        dao.clearAllDefault()
        dao.update(a.copy(isDefault = true))
    }

    suspend fun getDefault(): SearchAddressEntity? = dao.getDefault()

    suspend fun clearAll() = dao.clearAll()
}
