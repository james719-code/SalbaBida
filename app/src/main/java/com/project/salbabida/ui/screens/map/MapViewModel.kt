package com.project.salbabida.ui.screens.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.project.salbabida.data.database.entities.HomeLocation
import com.project.salbabida.data.database.entities.MarkerCategory
import com.project.salbabida.data.database.entities.OfflineMarker
import com.project.salbabida.data.model.PhilippineCities
import com.project.salbabida.data.preferences.UserPreferences
import com.project.salbabida.data.repository.EvacuationCenter
import com.project.salbabida.data.repository.MapRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** UI state for the map screen. */
data class MapUiState(
    // Location
    val homeLocation: HomeLocation? = null,
    val cityCenter: Pair<Double, Double> = Pair(13.6252, 123.1826),
    val currentMapCenter: Pair<Double, Double> = Pair(13.6252, 123.1826),
    val shouldRecenterMap: Boolean = false,
    val isFirstLoad: Boolean = true,

    // Markers
    val offlineMarkers: List<OfflineMarker> = emptyList(),
    val evacuationCenters: List<EvacuationCenter> = emptyList(),
    val nearestCenter: EvacuationCenter? = null,

    // Filters
    val showFilters: Boolean = false,
    val selectedCategories: Set<MarkerCategory> = MarkerCategory.entries.toSet(),
    val showOnlineShelters: Boolean = true,

    // Selection mode
    val isSelectingLocation: Boolean = false,
    val selectionMode: String? = null,
    val selectedPoint: Pair<Double, Double>? = null,

    // Dialogs / sheets
    val showAddMarkerSheet: Boolean = false,
    val showEditMarkerSheet: Boolean = false,
    val showSetHomeDialog: Boolean = false,
    val showSetWeatherLocationDialog: Boolean = false,
    val showPurgeSheltersDialog: Boolean = false,
    val showExtendedFab: Boolean = false,
    val selectedMarkerForEdit: OfflineMarker? = null,

    // User
    val userRole: String? = null,
    val selectedCityName: String? = null,

    // Errors
    val errorMessage: String? = null,
    val isPurging: Boolean = false
)

class MapViewModel(
    private val repository: MapRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    // Derived: markers filtered by selected categories + proximity
    val filteredMarkers: StateFlow<List<OfflineMarker>> = combine(
        _uiState
    ) { states ->
        val state = states[0]
        state.offlineMarkers.filter { marker ->
            state.selectedCategories.contains(marker.category) &&
                    calculateDistance(
                        state.currentMapCenter.first, state.currentMapCenter.second,
                        marker.latitude, marker.longitude
                    ) <= MARKER_VISIBILITY_RADIUS_KM
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        observeHomeLocation()
        observeOfflineMarkers()
        observeUserPreferences()
        loadEvacuationCenters()
    }

    // ── Observation ────────────────────────────────────────────────────

    private fun observeHomeLocation() {
        viewModelScope.launch {
            repository.observeHomeLocation().collect { home ->
                _uiState.update { it.copy(homeLocation = home) }
                recalculateCenter()
                recalculateNearestCenter()
            }
        }
    }

    private fun observeOfflineMarkers() {
        viewModelScope.launch {
            repository.observeAllMarkers().collect { markers ->
                _uiState.update { it.copy(offlineMarkers = markers) }
                recalculateNearestCenter()
            }
        }
    }

    private fun observeUserPreferences() {
        viewModelScope.launch {
            combine(
                userPreferences.selectedCity,
                userPreferences.userLatitude,
                userPreferences.userLongitude,
                userPreferences.userRole
            ) { city, lat, lon, role ->
                Quadruple(city, lat, lon, role)
            }.collect { (cityName, lat, lon, role) ->
                _uiState.update { it.copy(selectedCityName = cityName, userRole = role) }
                recalculateCenter(lat, lon)
            }
        }
    }

    private fun recalculateCenter(userLat: Double? = null, userLon: Double? = null) {
        val state = _uiState.value
        val selectedCity = state.selectedCityName?.let {
            PhilippineCities.findByName(it)
        } ?: PhilippineCities.getDefault()

        val home = state.homeLocation
        val newCenter = when {
            home != null -> Pair(home.latitude, home.longitude)
            userLat != null && userLon != null -> Pair(userLat, userLon)
            else -> Pair(selectedCity.latitude, selectedCity.longitude)
        }
        _uiState.update {
            it.copy(
                cityCenter = newCenter,
                currentMapCenter = newCenter,
                shouldRecenterMap = true
            )
        }
    }

    private fun loadEvacuationCenters() {
        viewModelScope.launch {
            val centers = repository.fetchEvacuationCenters()
            _uiState.update { it.copy(evacuationCenters = centers) }
            recalculateNearestCenter()
        }
    }

    private fun recalculateNearestCenter() {
        val state = _uiState.value
        val home = state.homeLocation ?: return
        val firestoreCenters = if (state.showOnlineShelters) {
            state.evacuationCenters.map { c ->
                c.copy(
                    distance = calculateDistance(
                        home.latitude, home.longitude,
                        c.latitude, c.longitude
                    )
                )
            }
        } else emptyList()

        val offlineEvacCenters = state.offlineMarkers
            .filter { it.category == MarkerCategory.EVACUATION_CENTER }
            .map { m ->
                EvacuationCenter(
                    name = m.name,
                    latitude = m.latitude,
                    longitude = m.longitude,
                    distance = calculateDistance(
                        home.latitude, home.longitude,
                        m.latitude, m.longitude
                    )
                )
            }

        val nearest = (firestoreCenters + offlineEvacCenters)
            .minByOrNull { it.distance ?: Double.MAX_VALUE }

        _uiState.update { it.copy(nearestCenter = nearest) }
    }

    // ── User actions ───────────────────────────────────────────────────

    fun onMapCenterChanged(lat: Double, lon: Double) {
        _uiState.update { it.copy(currentMapCenter = Pair(lat, lon)) }
    }

    fun onMapRecentered() {
        _uiState.update { it.copy(shouldRecenterMap = false, isFirstLoad = false) }
    }

    fun toggleFilters() {
        _uiState.update { it.copy(showFilters = !it.showFilters) }
    }

    fun toggleOnlineShelters() {
        _uiState.update { it.copy(showOnlineShelters = !it.showOnlineShelters) }
        recalculateNearestCenter()
    }

    fun toggleCategory(category: MarkerCategory) {
        _uiState.update { state ->
            val updated = state.selectedCategories.toMutableSet()
            if (updated.contains(category)) updated.remove(category) else updated.add(category)
            state.copy(selectedCategories = updated)
        }
    }

    fun startSelectingLocation(mode: String) {
        _uiState.update {
            it.copy(
                selectionMode = mode,
                isSelectingLocation = true,
                showExtendedFab = false
            )
        }
    }

    fun cancelSelection() {
        _uiState.update {
            it.copy(isSelectingLocation = false, selectionMode = null, selectedPoint = null)
        }
    }

    fun onLocationSelected(lat: Double, lon: Double) {
        val mode = _uiState.value.selectionMode
        _uiState.update {
            it.copy(
                selectedPoint = Pair(lat, lon),
                isSelectingLocation = false,
                showSetHomeDialog = mode == "home",
                showAddMarkerSheet = mode == "marker"
            )
        }
    }

    fun saveHomeLocation(isHouse: Boolean) {
        val point = _uiState.value.selectedPoint ?: return
        viewModelScope.launch {
            val home = HomeLocation(
                latitude = point.first,
                longitude = point.second,
                isHouse = isHouse,
                name = if (isHouse) "My Home" else "Saved Location"
            )
            repository.saveHomeLocation(home)
            _uiState.update {
                it.copy(showSetHomeDialog = false, selectedPoint = null)
            }
        }
    }

    fun saveMarker(name: String, category: MarkerCategory, notes: String?) {
        val point = _uiState.value.selectedPoint ?: return
        viewModelScope.launch {
            val marker = OfflineMarker(
                name = name,
                latitude = point.first,
                longitude = point.second,
                category = category,
                notes = notes
            )
            repository.insertMarker(marker)
            _uiState.update {
                it.copy(showAddMarkerSheet = false, selectedPoint = null)
            }
        }
    }

    fun selectMarkerForEdit(marker: OfflineMarker) {
        _uiState.update {
            it.copy(selectedMarkerForEdit = marker, showEditMarkerSheet = true)
        }
    }

    fun updateMarker(name: String, category: MarkerCategory, notes: String?) {
        val original = _uiState.value.selectedMarkerForEdit ?: return
        viewModelScope.launch {
            repository.updateMarker(original.copy(name = name, category = category, notes = notes))
            _uiState.update {
                it.copy(showEditMarkerSheet = false, selectedMarkerForEdit = null)
            }
        }
    }

    fun deleteSelectedMarker() {
        val marker = _uiState.value.selectedMarkerForEdit ?: return
        viewModelScope.launch {
            repository.deleteMarker(marker)
            _uiState.update {
                it.copy(showEditMarkerSheet = false, selectedMarkerForEdit = null)
            }
        }
    }

    fun setWeatherLocation() {
        val center = _uiState.value.currentMapCenter
        viewModelScope.launch {
            // Resolve a sensible label; fall back to coords
            val label = _uiState.value.selectedCityName ?: "Custom Location"
            userPreferences.setWeatherLocation(center.first, center.second, label)
            _uiState.update { it.copy(showSetWeatherLocationDialog = false) }
        }
    }

    fun purgeOnlineShelters() {
        _uiState.update { it.copy(isPurging = true) }
        viewModelScope.launch {
            repository.purgeOnlineEvacuationCenters().fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            evacuationCenters = emptyList(),
                            nearestCenter = null,
                            showPurgeSheltersDialog = false,
                            isPurging = false
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            errorMessage = e.message ?: "Failed to purge shelters",
                            showPurgeSheltersDialog = false,
                            isPurging = false
                        )
                    }
                }
            )
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun recenterToHome() {
        val home = _uiState.value.homeLocation
        if (home != null) {
            _uiState.update {
                it.copy(
                    cityCenter = Pair(home.latitude, home.longitude),
                    shouldRecenterMap = true
                )
            }
        } else {
            _uiState.update { it.copy(shouldRecenterMap = true) }
        }
    }

    fun recenterToNearestShelter() {
        val center = _uiState.value.nearestCenter ?: return
        _uiState.update {
            it.copy(
                cityCenter = Pair(center.latitude, center.longitude),
                shouldRecenterMap = true
            )
        }
    }

    // Show/hide helpers
    fun showExtendedFab(show: Boolean) {
        _uiState.update { it.copy(showExtendedFab = show) }
    }

    fun showSetWeatherLocationDialog(show: Boolean) {
        _uiState.update { it.copy(showSetWeatherLocationDialog = show) }
    }

    fun showPurgeSheltersDialog(show: Boolean) {
        _uiState.update { it.copy(showPurgeSheltersDialog = show) }
    }

    fun dismissAddMarkerSheet() {
        _uiState.update { it.copy(showAddMarkerSheet = false, selectedPoint = null) }
    }

    fun dismissEditMarkerSheet() {
        _uiState.update { it.copy(showEditMarkerSheet = false, selectedMarkerForEdit = null) }
    }

    fun dismissSetHomeDialog() {
        _uiState.update { it.copy(showSetHomeDialog = false) }
    }

    // ── Helpers ────────────────────────────────────────────────────────

    companion object {
        private const val MARKER_VISIBILITY_RADIUS_KM = 50.0

        fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
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

    class Factory(
        private val repository: MapRepository,
        private val userPreferences: UserPreferences
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MapViewModel(repository, userPreferences) as T
        }
    }
}

/** Tiny helper – avoids adding an external dependency for combining four flows. */
private data class Quadruple<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
