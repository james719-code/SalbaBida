package com.project.salbabida

import android.app.Application
import android.content.Context
import android.os.Environment
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration as OsmConfig
import java.io.File
import javax.inject.Inject

@HiltAndroidApp
class SalbaBidaApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var syncManager: com.project.salbabida.data.sync.SyncManager

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Configure OSMDroid
        configureOsmDroid()

        // Schedule periodic marker sync
        syncManager.schedulePeriodicSync()

        // Auto-sync when connectivity returns
        val connectivityObserver = com.project.salbabida.data.sync.ConnectivityObserver(this)
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            connectivityObserver.isConnected.collect { connected ->
                if (connected) {
                    syncManager.syncNow()
                }
            }
        }
    }
    
    private fun configureOsmDroid() {
        OsmConfig.getInstance().apply {
            load(this@SalbaBidaApplication, getSharedPreferences("osm_prefs", Context.MODE_PRIVATE))
            userAgentValue = packageName
            
            // Set up persistent tile caching for offline maps
            val cacheDir = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "osmdroid")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            osmdroidBasePath = cacheDir
            osmdroidTileCache = File(cacheDir, "tiles")
            
            // Set cache size (100MB for offline tiles)
            tileFileSystemCacheMaxBytes = 100L * 1024 * 1024
            tileFileSystemCacheTrimBytes = 80L * 1024 * 1024
        }
    }
    
    companion object {
        private lateinit var instance: SalbaBidaApplication
        
        fun getInstance(): SalbaBidaApplication = instance
    }
}
