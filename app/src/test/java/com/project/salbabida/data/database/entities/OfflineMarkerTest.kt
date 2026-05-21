package com.project.salbabida.data.database.entities

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class OfflineMarkerTest {
    @Test
    fun newOfflineMarkerDefaultsToPendingSync() {
        val marker = OfflineMarker(
            name = "Barangay Hall",
            latitude = 13.6252,
            longitude = 123.1826,
            category = MarkerCategory.EVACUATION_CENTER
        )

        assertNotNull(marker.id)
        assertEquals(SyncStatus.PENDING, marker.syncStatus)
    }
}
