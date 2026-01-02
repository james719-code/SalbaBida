package com.project.salbabida.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weather_cache")
data class WeatherCache(
    @PrimaryKey
    val city: String,
    val temperature: Double,
    val feelsLike: Double,
    val humidity: Int,
    val pressure: Int,
    val visibility: Int,
    val windSpeed: Double,
    val windDeg: Int,
    val windGust: Double?,
    val cloudiness: Int,
    val country: String,
    val description: String,
    val icon: String,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    fun isExpired(): Boolean {
        val twelveHoursInMillis = 12 * 60 * 60 * 1000L
        return System.currentTimeMillis() - lastUpdated > twelveHoursInMillis
    }
}
