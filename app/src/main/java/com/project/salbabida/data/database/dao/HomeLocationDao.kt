package com.project.salbabida.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.project.salbabida.data.database.entities.HomeLocation
import kotlinx.coroutines.flow.Flow

@Dao
interface HomeLocationDao {
    @Query("SELECT * FROM home_location LIMIT 1")
    suspend fun getHomeLocation(): HomeLocation?
    
    @Query("SELECT * FROM home_location LIMIT 1")
    fun observeHomeLocation(): Flow<HomeLocation?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHomeLocation(location: HomeLocation)
    
    @Update
    suspend fun updateHomeLocation(location: HomeLocation)
    
    @Query("DELETE FROM home_location")
    suspend fun deleteHomeLocation()
}
