package com.project.salbabida.data.risk

import com.project.salbabida.data.database.entities.WeatherCache

enum class FloodRiskLevel {
    LOW,
    MODERATE,
    HIGH,
    EMERGENCY
}

data class FloodRiskAssessment(
    val level: FloodRiskLevel,
    val score: Int,
    val reasons: List<String>,
    val recommendedAction: String
)

object FloodRiskScorer {
    fun assess(
        weather: WeatherCache,
        nearestFloodZoneDistanceKm: Double?,
        manualAlertActive: Boolean = false
    ): FloodRiskAssessment {
        val reasons = mutableListOf<String>()
        var score = 0
        val description = weather.description.lowercase()

        when {
            manualAlertActive -> {
                score += 55
                reasons += "Manual barangay alert is active"
            }
            description.contains("thunderstorm") || description.contains("storm") -> {
                score += 40
                reasons += "Storm conditions reported"
            }
            description.contains("heavy rain") || description.contains("rain") -> {
                score += 28
                reasons += "Rainfall conditions reported"
            }
            description.contains("drizzle") -> {
                score += 15
                reasons += "Light rain or drizzle reported"
            }
        }

        if (weather.humidity >= 90) {
            score += 15
            reasons += "Humidity is very high"
        } else if (weather.humidity >= 80) {
            score += 8
            reasons += "Humidity is elevated"
        }

        if (weather.cloudiness >= 85) {
            score += 12
            reasons += "Cloud cover is heavy"
        }

        if (weather.windSpeed >= 14.0) {
            score += 10
            reasons += "Wind speed may worsen travel conditions"
        }

        nearestFloodZoneDistanceKm?.let { distance ->
            when {
                distance <= 0.5 -> {
                    score += 25
                    reasons += "Home is within 500 m of a flood zone"
                }
                distance <= 2.0 -> {
                    score += 15
                    reasons += "Home is near a flood zone"
                }
                distance <= 5.0 -> {
                    score += 6
                    reasons += "Flood zone exists within 5 km"
                }
            }
        }

        val boundedScore = score.coerceIn(0, 100)
        val level = when {
            boundedScore >= 75 -> FloodRiskLevel.EMERGENCY
            boundedScore >= 55 -> FloodRiskLevel.HIGH
            boundedScore >= 30 -> FloodRiskLevel.MODERATE
            else -> FloodRiskLevel.LOW
        }

        return FloodRiskAssessment(
            level = level,
            score = boundedScore,
            reasons = reasons.ifEmpty { listOf("No immediate flood indicators detected") },
            recommendedAction = recommendationFor(level)
        )
    }

    private fun recommendationFor(level: FloodRiskLevel): String {
        return when (level) {
            FloodRiskLevel.LOW -> "Monitor weather updates and keep emergency supplies ready."
            FloodRiskLevel.MODERATE -> "Stay alert, check evacuation routes, and avoid flood-prone roads."
            FloodRiskLevel.HIGH -> "Prepare to evacuate and follow barangay or DRRMO advisories."
            FloodRiskLevel.EMERGENCY -> "Move to a safe area or evacuation center immediately if advised."
        }
    }
}
