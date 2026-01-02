package com.project.salbabida.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class MarkerCategory {
    EVACUATION_CENTER,
    FLOOD_ZONE,
    SAFE_AREA,
    RESOURCE_CENTER
}

enum class SyncStatus {
    PENDING,
    SYNCED,
    FAILED
}

@Entity(tableName = "offline_markers")
data class OfflineMarker(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val category: MarkerCategory,
    val notes: String? = null,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val syncedAt: Long? = null,
    val firestoreId: String? = null
)
