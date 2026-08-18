package com.shijian.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.shijian.app.data.db.dao.CategoryDao
import com.shijian.app.data.db.dao.FoodPoiDao
import com.shijian.app.data.db.dao.NewsDao
import com.shijian.app.data.db.dao.SearchAddressDao
import com.shijian.app.data.db.dao.TransactionDao
import com.shijian.app.data.db.entity.CategoryEntity
import com.shijian.app.data.db.entity.FoodPoiEntity
import com.shijian.app.data.db.entity.NewsConfigEntity
import com.shijian.app.data.db.entity.NewsItemEntity
import com.shijian.app.data.db.entity.SearchAddressEntity
import com.shijian.app.data.db.entity.TransactionEntity

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        FoodPoiEntity::class,
        SearchAddressEntity::class,
        NewsConfigEntity::class,
        NewsItemEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun foodPoiDao(): FoodPoiDao
    abstract fun searchAddressDao(): SearchAddressDao
    abstract fun newsDao(): NewsDao
}
