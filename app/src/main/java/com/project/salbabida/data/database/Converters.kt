package com.project.salbabida.data.database

import androidx.room.TypeConverter
import com.project.salbabida.data.database.entities.MarkerCategory
import com.project.salbabida.data.database.entities.SyncStatus

class Converters {
    @TypeConverter
    fun fromMarkerCategory(category: MarkerCategory): String = category.name
    
    @TypeConverter
    fun toMarkerCategory(value: String): MarkerCategory = MarkerCategory.valueOf(value)
    
    @TypeConverter
    fun fromSyncStatus(status: SyncStatus): String = status.name
    
    @TypeConverter
    fun toSyncStatus(value: String): SyncStatus = SyncStatus.valueOf(value)
}
