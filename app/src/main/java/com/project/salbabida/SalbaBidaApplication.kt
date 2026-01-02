package com.project.salbabida

import android.app.Application
import android.content.Context
import android.os.Environment
import com.project.salbabida.data.database.SalbaBidaDatabase
import com.project.salbabida.data.preferences.UserPreferences
import org.osmdroid.config.Configuration
import java.io.File

class SalbaBidaApplication : Application() {
    
    lateinit var database: SalbaBidaDatabase
        private set
    
    lateinit var userPreferences: UserPreferences
        private set
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Initialize database
        database = SalbaBidaDatabase.getInstance(this)
        
        // Initialize preferences
        userPreferences = UserPreferences(this)
        
        // Configure OSMDroid
        configureOsmDroid()
    }
    
    private fun configureOsmDroid() {
        Configuration.getInstance().apply {
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
