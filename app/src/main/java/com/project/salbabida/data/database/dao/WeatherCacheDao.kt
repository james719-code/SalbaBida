package com.project.salbabida.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.project.salbabida.data.database.entities.WeatherCache
import kotlinx.coroutines.flow.Flow

@Dao
interface WeatherCacheDao {
    @Query("SELECT * FROM weather_cache WHERE city = :city LIMIT 1")
    suspend fun getWeatherForCity(city: String): WeatherCache?
    
    @Query("SELECT * FROM weather_cache WHERE city = :city LIMIT 1")
    fun observeWeatherForCity(city: String): Flow<WeatherCache?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeather(weather: WeatherCache)
    
    @Query("DELETE FROM weather_cache WHERE city = :city")
    suspend fun deleteWeatherForCity(city: String)
    
    @Query("DELETE FROM weather_cache")
    suspend fun clearAll()
}
