package com.project.salbabida.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.project.salbabida.data.database.entities.OfflineMarker
import com.project.salbabida.data.database.entities.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineMarkerDao {
    @Query("SELECT * FROM offline_markers ORDER BY createdAt DESC")
    fun observeAllMarkers(): Flow<List<OfflineMarker>>
    
    @Query("SELECT * FROM offline_markers ORDER BY createdAt DESC")
    suspend fun getAllMarkers(): List<OfflineMarker>
    
    @Query("SELECT * FROM offline_markers WHERE syncStatus = :status")
    suspend fun getMarkersByStatus(status: SyncStatus): List<OfflineMarker>
    
    @Query("SELECT * FROM offline_markers WHERE syncStatus = 'PENDING'")
    suspend fun getPendingMarkers(): List<OfflineMarker>
    
    @Query("SELECT * FROM offline_markers WHERE id = :id")
    suspend fun getMarkerById(id: String): OfflineMarker?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMarker(marker: OfflineMarker)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMarkers(markers: List<OfflineMarker>)
    
    @Update
    suspend fun updateMarker(marker: OfflineMarker)
    
    @Query("UPDATE offline_markers SET syncStatus = :status, syncedAt = :syncedAt, firestoreId = :firestoreId WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: SyncStatus, syncedAt: Long?, firestoreId: String?)
    
    @Delete
    suspend fun deleteMarker(marker: OfflineMarker)
    
    @Query("DELETE FROM offline_markers WHERE id = :id")
    suspend fun deleteMarkerById(id: String)
    
    @Query("DELETE FROM offline_markers")
    suspend fun clearAll()
}
