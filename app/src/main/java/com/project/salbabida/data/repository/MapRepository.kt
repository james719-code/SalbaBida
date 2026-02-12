package com.project.salbabida.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.project.salbabida.data.database.dao.HomeLocationDao
import com.project.salbabida.data.database.dao.OfflineMarkerDao
import com.project.salbabida.data.database.entities.HomeLocation
import com.project.salbabida.data.database.entities.OfflineMarker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

data class EvacuationCenter(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val distance: Double? = null
)

class MapRepository(
    private val homeLocationDao: HomeLocationDao,
    private val offlineMarkerDao: OfflineMarkerDao,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    fun observeAllMarkers(): Flow<List<OfflineMarker>> =
        offlineMarkerDao.observeAllMarkers()

    fun observeHomeLocation(): Flow<HomeLocation?> =
        homeLocationDao.observeHomeLocation()

    suspend fun getHomeLocation(): HomeLocation? =
        homeLocationDao.getHomeLocation()

    suspend fun saveHomeLocation(location: HomeLocation) {
        homeLocationDao.deleteHomeLocation()
        homeLocationDao.insertHomeLocation(location)
    }

    suspend fun insertMarker(marker: OfflineMarker) =
        offlineMarkerDao.insertMarker(marker)

    suspend fun updateMarker(marker: OfflineMarker) =
        offlineMarkerDao.updateMarker(marker)

    suspend fun deleteMarker(marker: OfflineMarker) =
        offlineMarkerDao.deleteMarker(marker)

    suspend fun fetchEvacuationCenters(): List<EvacuationCenter> {
        return try {
            val snapshot = firestore
                .collection("evacuation_centers")
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                val name = doc.getString("name") ?: return@mapNotNull null
                val place = doc.getGeoPoint("place") ?: return@mapNotNull null
                EvacuationCenter(name, place.latitude, place.longitude)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun purgeOnlineEvacuationCenters(): Result<Unit> {
        return try {
            val snapshot = firestore
                .collection("evacuation_centers")
                .get()
                .await()
            snapshot.documents.forEach { doc ->
                doc.reference.delete().await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
