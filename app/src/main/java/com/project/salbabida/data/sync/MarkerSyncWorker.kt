package com.project.salbabida.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.project.salbabida.data.database.dao.OfflineMarkerDao
import com.project.salbabida.data.database.entities.SyncStatus
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.tasks.await

/**
 * WorkManager worker that syncs pending/failed markers to Firestore.
 * Uses exponential backoff on failure via Result.retry().
 */
@HiltWorker
class MarkerSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val offlineMarkerDao: OfflineMarkerDao,
    private val firestore: FirebaseFirestore
) : CoroutineWorker(appContext, params) {

    companion object {
        const val TAG = "MarkerSyncWorker"
        const val WORK_NAME = "marker_sync"
        const val PERIODIC_WORK_NAME = "marker_periodic_sync"
    }

    override suspend fun doWork(): Result {
        val pendingMarkers = offlineMarkerDao.getPendingMarkers()
        val failedMarkers = offlineMarkerDao.getMarkersByStatus(SyncStatus.FAILED)
        val markersToSync = pendingMarkers + failedMarkers

        if (markersToSync.isEmpty()) {
            return Result.success()
        }

        var allSucceeded = true
        var anyNetworkError = false

        for (marker in markersToSync) {
            try {
                val data = hashMapOf(
                    "name" to marker.name,
                    "place" to GeoPoint(marker.latitude, marker.longitude),
                    "category" to marker.category.name,
                    "notes" to (marker.notes ?: ""),
                    "createdAt" to marker.createdAt,
                    "localId" to marker.id
                )

                val docRef = if (marker.firestoreId != null) {
                    // Update existing document
                    firestore.collection("markers")
                        .document(marker.firestoreId)
                        .set(data)
                        .await()
                    marker.firestoreId
                } else {
                    // Create new document
                    val ref = firestore.collection("markers")
                        .add(data)
                        .await()
                    ref.id
                }

                offlineMarkerDao.updateSyncStatus(
                    id = marker.id,
                    status = SyncStatus.SYNCED,
                    syncedAt = System.currentTimeMillis(),
                    firestoreId = docRef
                )

            } catch (e: Exception) {
                allSucceeded = false

                // Check if it's a network error (should retry) vs data error (should mark failed)
                if (e is java.io.IOException || e.cause is java.io.IOException) {
                    anyNetworkError = true
                } else {
                    offlineMarkerDao.updateSyncStatus(
                        id = marker.id,
                        status = SyncStatus.FAILED,
                        syncedAt = null,
                        firestoreId = marker.firestoreId
                    )
                }
            }
        }

        return when {
            allSucceeded -> Result.success()
            anyNetworkError -> Result.retry()
            else -> Result.success()
        }
    }
}
