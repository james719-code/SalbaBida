package com.project.salbabida.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central manager for offline marker synchronization via WorkManager.
 *
 * Provides:
 * - One-time sync (triggered after saving a marker or when connectivity returns)
 * - Periodic sync (fallback every 1 hour when network is available)
 * - Sync status observation for the UI
 */
@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val workManager = WorkManager.getInstance(context)

    private val networkConstraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    /**
     * Enqueue a one-time sync. Use after saving/editing a marker.
     * If a sync is already running, this call is ignored (KEEP policy).
     */
    fun syncNow() {
        val request = OneTimeWorkRequestBuilder<MarkerSyncWorker>()
            .setConstraints(networkConstraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag(MarkerSyncWorker.TAG)
            .build()

        workManager.enqueueUniqueWork(
            MarkerSyncWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    /**
     * Schedule a periodic sync every hour as a fallback.
     * Only runs when network is available.
     */
    fun schedulePeriodicSync() {
        val request = PeriodicWorkRequestBuilder<MarkerSyncWorker>(
            1, TimeUnit.HOURS,
            15, TimeUnit.MINUTES  // flex interval
        )
            .setConstraints(networkConstraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag(MarkerSyncWorker.TAG)
            .build()

        workManager.enqueueUniquePeriodicWork(
            MarkerSyncWorker.PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    /**
     * Observe sync work status for UI indicators.
     * Returns true when a sync operation is currently running.
     */
    fun observeSyncStatus(): Flow<Boolean> {
        return workManager.getWorkInfosByTagFlow(MarkerSyncWorker.TAG)
            .map { workInfos ->
                workInfos.any { it.state == WorkInfo.State.RUNNING }
            }
    }

    /**
     * Cancel all pending sync work.
     */
    fun cancelAll() {
        workManager.cancelAllWorkByTag(MarkerSyncWorker.TAG)
    }
}
