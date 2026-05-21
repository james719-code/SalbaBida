package com.project.salbabida.data.database.entities

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherCacheTest {
    @Test
    fun weatherCacheYoungerThanTwelveHoursIsFresh() {
        val cache = weatherCache(lastUpdated = System.currentTimeMillis() - 11 * 60 * 60 * 1000L)

        assertFalse(cache.isExpired())
    }

    @Test
    fun weatherCacheOlderThanTwelveHoursIsExpired() {
        val cache = weatherCache(lastUpdated = System.currentTimeMillis() - 13 * 60 * 60 * 1000L)

        assertTrue(cache.isExpired())
    }

    private fun weatherCache(lastUpdated: Long): WeatherCache {
        return WeatherCache(
            city = "Sorsogon City",
            temperature = 28.0,
            feelsLike = 30.0,
            humidity = 80,
            pressure = 1009,
            visibility = 10_000,
            windSpeed = 4.0,
            windDeg = 90,
            windGust = null,
            cloudiness = 50,
            country = "PH",
            description = "clouds",
            icon = "03d",
            lastUpdated = lastUpdated
        )
    }
}
