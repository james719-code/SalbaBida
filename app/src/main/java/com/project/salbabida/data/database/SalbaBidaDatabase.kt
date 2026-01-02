package com.project.salbabida.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.project.salbabida.data.database.dao.HomeLocationDao
import com.project.salbabida.data.database.dao.OfflineMarkerDao
import com.project.salbabida.data.database.dao.WeatherCacheDao
import com.project.salbabida.data.database.entities.HomeLocation
import com.project.salbabida.data.database.entities.OfflineMarker
import com.project.salbabida.data.database.entities.WeatherCache

@Database(
    entities = [
        WeatherCache::class,
        HomeLocation::class,
        OfflineMarker::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SalbaBidaDatabase : RoomDatabase() {
    abstract fun weatherCacheDao(): WeatherCacheDao
    abstract fun homeLocationDao(): HomeLocationDao
    abstract fun offlineMarkerDao(): OfflineMarkerDao
    
    companion object {
        @Volatile
        private var INSTANCE: SalbaBidaDatabase? = null
        
        fun getInstance(context: Context): SalbaBidaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SalbaBidaDatabase::class.java,
                    "salbabida_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
