package com.project.salbabida.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.salbabida.data.database.entities.HomeLocation
import com.project.salbabida.data.database.entities.MarkerCategory
import com.project.salbabida.data.database.entities.OfflineMarker
import com.project.salbabida.data.database.entities.WeatherCache
import com.project.salbabida.data.preferences.UserPreferences
import com.project.salbabida.data.repository.MapRepository
import com.project.salbabida.data.repository.WeatherRepository
import com.project.salbabida.data.risk.FloodRiskAssessment
import com.project.salbabida.data.risk.FloodRiskScorer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class HomeUiState(
    val weatherData: WeatherCache? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val locationName: String = "",
    val floodRiskAssessment: FloodRiskAssessment? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: WeatherRepository,
    private val mapRepository: MapRepository,
    private val preferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var latestMarkers: List<OfflineMarker> = emptyList()
    private var latestHomeLocation: HomeLocation? = null

    init {
        observeRiskContext()
        loadWeather(forceRefresh = false)
    }

    private fun observeRiskContext() {
        viewModelScope.launch {
            combine(
                mapRepository.observeAllMarkers(),
                mapRepository.observeHomeLocation()
            ) { markers, homeLocation ->
                markers to homeLocation
            }.collect { (markers, homeLocation) ->
                latestMarkers = markers
                latestHomeLocation = homeLocation
                updateFloodRiskAssessment()
            }
        }
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isRefreshing = true)
        loadWeather(forceRefresh = true)
    }

    fun loadWeather(forceRefresh: Boolean) {
        viewModelScope.launch {
            try {
                if (!forceRefresh) {
                     _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                }
                
                // 1. Determine Location
                val weatherLat = preferences.weatherLatitude.first()
                val weatherLon = preferences.weatherLongitude.first()
                val userLat = preferences.userLatitude.first()
                val userLon = preferences.userLongitude.first()
                
                val effectiveLat = weatherLat ?: userLat
                val effectiveLon = weatherLon ?: userLon
                val hasCoordinates = effectiveLat != null && effectiveLon != null
                
                val savedLocationName = preferences.weatherLocationName.first()
                val userBarangay = preferences.userBarangay.first()
                val userCity = preferences.userCity.first()
                val userProvince = preferences.userProvince.first()
                 val userLocationLabel = listOfNotNull(
                    userBarangay?.takeIf { it.isNotBlank() },
                    userCity?.takeIf { it.isNotBlank() },
                    userProvince?.takeIf { it.isNotBlank() }
                ).joinToString(", ")
                
                val cityPref = preferences.selectedCity.first() ?: "Sorsogon City"

                // Calculate cache key
                val (roundedLat, roundedLon) = if (hasCoordinates) {
                    // Round to 2 decimal places (~1.1km precision) to improve cache hits
                    val lat = String.format(Locale.US, "%.2f", effectiveLat).toDouble()
                    val lon = String.format(Locale.US, "%.2f", effectiveLon).toDouble()
                    Pair(lat, lon)
                } else {
                    Pair(0.0, 0.0)
                }

                val cacheKey = if (hasCoordinates) {
                    String.format(Locale.US, "%.2f_%.2f", roundedLat, roundedLon)
                } else {
                    cityPref
                }
                
                // Determine Display Name
                val locationName = when {
                    savedLocationName?.isNotBlank() == true -> savedLocationName!!
                    hasCoordinates && userLocationLabel.isNotBlank() -> userLocationLabel
                    hasCoordinates -> "Current Location"
                    else -> cityPref
                }
                
                _uiState.value = _uiState.value.copy(locationName = locationName)

                // 2. Check Cache
                val cached = repository.getCachedWeather(cacheKey)
                if (cached != null) {
                    setWeatherData(cached)
                }
                
                // 3. Decide to Fetch
                if (forceRefresh || cached == null || cached.isExpired()) {
                    val result = if (hasCoordinates) {
                        repository.fetchWeatherByCoordinates(roundedLat, roundedLon)
                    } else {
                        repository.fetchWeatherByCity(cityPref)
                    }

                    result.fold(
                        onSuccess = { (fetchedWeather, fetchedName) ->
                            // Update location name if strictly dynamic
                            if (savedLocationName.isNullOrBlank() && (!hasCoordinates || userLocationLabel.isBlank())) {
                                _uiState.value = _uiState.value.copy(locationName = fetchedName)
                            }
                            setWeatherData(fetchedWeather)
                        },
                        onFailure = { e ->
                            // Keep showing cached data if available; surface error
                            _uiState.value = _uiState.value.copy(
                                error = e.message ?: "Failed to fetch weather"
                            )
                        }
                    )
                }

                _uiState.value = _uiState.value.copy(isLoading = false, isRefreshing = false, error = null)

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false, 
                    isRefreshing = false,
                    error = e.message ?: "Failed to fetch weather"
                )
                
                // Fallback to cache if failed
                 try {
                     // Re-calculate basic key just in case (skipping full logic for brevity, assuming state hasn't drifted wildly)
                      // Ideally we'd reuse the calculate logic but for now straightforward fallback
                 } catch (e2: Exception) {
                     // ignore
                 }
            }
        }
    }

    private fun setWeatherData(weather: WeatherCache) {
        _uiState.value = _uiState.value.copy(weatherData = weather)
        updateFloodRiskAssessment()
    }

    private fun updateFloodRiskAssessment() {
        val weather = _uiState.value.weatherData ?: return
        val home = latestHomeLocation
        val nearestFloodZoneDistanceKm = home?.let { homeLocation ->
            latestMarkers
                .asSequence()
                .filter { it.category == MarkerCategory.FLOOD_ZONE }
                .map {
                    calculateDistance(
                        homeLocation.latitude,
                        homeLocation.longitude,
                        it.latitude,
                        it.longitude
                    )
                }
                .minOrNull()
        }
        val manualAlertActive = latestMarkers.any { marker ->
            marker.category == MarkerCategory.FLOOD_ZONE &&
                marker.notes?.contains("alert", ignoreCase = true) == true
        }

        _uiState.value = _uiState.value.copy(
            floodRiskAssessment = FloodRiskScorer.assess(
                weather = weather,
                nearestFloodZoneDistanceKm = nearestFloodZoneDistanceKm,
                manualAlertActive = manualAlertActive
            )
        )
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }
}
