package com.project.salbabida.data.risk

import com.project.salbabida.data.database.entities.WeatherCache
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FloodRiskScorerTest {
    @Test
    fun clearWeatherFarFromFloodZoneReturnsLowRisk() {
        val assessment = FloodRiskScorer.assess(
            weather = weather(description = "clear sky", humidity = 60, cloudiness = 10),
            nearestFloodZoneDistanceKm = 8.0
        )

        assertEquals(FloodRiskLevel.LOW, assessment.level)
        assertTrue(assessment.score < 30)
    }

    @Test
    fun rainNearFloodZoneReturnsModerateRisk() {
        val assessment = FloodRiskScorer.assess(
            weather = weather(description = "moderate rain", humidity = 72, cloudiness = 55),
            nearestFloodZoneDistanceKm = 4.0
        )

        assertEquals(FloodRiskLevel.MODERATE, assessment.level)
        assertTrue(assessment.reasons.any { it.contains("flood zone", ignoreCase = true) })
    }

    @Test
    fun manualAlertWithStormReturnsEmergencyRisk() {
        val assessment = FloodRiskScorer.assess(
            weather = weather(description = "thunderstorm", humidity = 93, cloudiness = 96),
            nearestFloodZoneDistanceKm = 0.2,
            manualAlertActive = true
        )

        assertEquals(FloodRiskLevel.EMERGENCY, assessment.level)
        assertTrue(assessment.score >= 75)
    }

    private fun weather(
        description: String,
        humidity: Int,
        cloudiness: Int,
        windSpeed: Double = 3.0
    ): WeatherCache {
        return WeatherCache(
            city = "test",
            temperature = 28.0,
            feelsLike = 31.0,
            humidity = humidity,
            pressure = 1008,
            visibility = 10_000,
            windSpeed = windSpeed,
            windDeg = 90,
            windGust = null,
            cloudiness = cloudiness,
            country = "PH",
            description = description,
            icon = "10d"
        )
    }
}
