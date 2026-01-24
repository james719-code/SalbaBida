package com.project.salbabida.ui.screens.map

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import com.google.firebase.firestore.FirebaseFirestore
import com.project.salbabida.SalbaBidaApplication
import com.project.salbabida.data.database.entities.HomeLocation
import com.project.salbabida.data.database.entities.MarkerCategory
import com.project.salbabida.data.database.entities.OfflineMarker
import com.project.salbabida.data.model.PhilippineCities
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

private val defaultCoordinates = Pair(13.6252, 123.1826)
private const val MARKER_VISIBILITY_RADIUS_KM = 50.0

data class EvacuationCenter(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val distance: Double? = null
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MapScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val app = SalbaBidaApplication.getInstance()
    val homeLocationDao = app.database.homeLocationDao()
    val offlineMarkerDao = app.database.offlineMarkerDao()
    val userPreferences = app.userPreferences
    
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var homeLocation by remember { mutableStateOf<HomeLocation?>(null) }
    val evacuationCenters = remember { mutableStateListOf<EvacuationCenter>() }
    val offlineMarkers = remember { mutableStateListOf<OfflineMarker>() }
    var nearestCenter by remember { mutableStateOf<EvacuationCenter?>(null) }
    
    var showAddMarkerSheet by remember { mutableStateOf(false) }
    var showEditMarkerSheet by remember { mutableStateOf(false) }
    var showDeleteMarkerDialog by remember { mutableStateOf(false) }
    var showSetHomeDialog by remember { mutableStateOf(false) }
    var selectedPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var selectedMarkerForEdit by remember { mutableStateOf<OfflineMarker?>(null) }
    var isSelectingLocation by remember { mutableStateOf(false) }
    var selectionMode by remember { mutableStateOf<String?>(null) }
    
    // Filter state
    var showFilters by remember { mutableStateOf(false) }
    val selectedCategories = remember { mutableStateListOf<MarkerCategory>().apply { 
        addAll(MarkerCategory.entries) 
    } }
    var showOnlineShelters by remember { mutableStateOf(true) }
    
    var showExtendedFab by remember { mutableStateOf(false) }
    var showSetWeatherLocationDialog by remember { mutableStateOf(false) }
    var showPurgeSheltersDialog by remember { mutableStateOf(false) }
    
    val selectedCityName by userPreferences.selectedCity.collectAsState(initial = null)
    val userLat by userPreferences.userLatitude.collectAsState(initial = null)
    val userLon by userPreferences.userLongitude.collectAsState(initial = null)
    val userBarangay by userPreferences.userBarangay.collectAsState(initial = null)
    val userCity by userPreferences.userCity.collectAsState(initial = null)
    val userProvince by userPreferences.userProvince.collectAsState(initial = null)
    val userRole by userPreferences.userRole.collectAsState(initial = null)
    
    val selectedCity = remember(selectedCityName) {
        selectedCityName?.let { PhilippineCities.findByName(it) } ?: PhilippineCities.getDefault()
    }
    var cityCenter by remember { mutableStateOf(Pair(selectedCity.latitude, selectedCity.longitude)) }
    var shouldRecenterMap by remember { mutableStateOf(false) }
    
    // Track current map center for proximity-based marker filtering
    var currentMapCenter by remember { mutableStateOf(Pair(selectedCity.latitude, selectedCity.longitude)) }
    
    // Update center when selected city, user location, or home location changes
    LaunchedEffect(selectedCity, userLat, userLon, homeLocation) {
        cityCenter = when {
            homeLocation != null -> Pair(homeLocation!!.latitude, homeLocation!!.longitude)
            userLat != null && userLon != null -> Pair(userLat!!, userLon!!)
            else -> Pair(selectedCity.latitude, selectedCity.longitude)
        }
        currentMapCenter = cityCenter
        shouldRecenterMap = true
    }
    
    var isFirstLoad by remember { mutableStateOf(true) }
    
    // Recenter map when location changes
    LaunchedEffect(shouldRecenterMap, mapView) {
        if (shouldRecenterMap && mapView != null) {
            if (isFirstLoad) {
                mapView?.controller?.setCenter(GeoPoint(cityCenter.first, cityCenter.second))
                mapView?.controller?.setZoom(14.0)
                isFirstLoad = false
            } else {
                mapView?.controller?.animateTo(
                    GeoPoint(cityCenter.first, cityCenter.second),
                    18.0,
                    1000L
                )
            }
            shouldRecenterMap = false
        }
    }
    
    // Load home location
    LaunchedEffect(Unit) {
        homeLocation = homeLocationDao.getHomeLocation()
    }
    
    // Reload home location when saved
    var homeLocationTrigger by remember { mutableStateOf(0) }
    LaunchedEffect(homeLocationTrigger) {
        if (homeLocationTrigger > 0) {
            homeLocation = homeLocationDao.getHomeLocation()
        }
    }

    
    // Load offline markers
    LaunchedEffect(Unit) {
        offlineMarkerDao.observeAllMarkers().collect { markers ->
            offlineMarkers.clear()
            offlineMarkers.addAll(markers)
        }
    }
    
    // Fetch evacuation centers from Firestore
    LaunchedEffect(Unit) {
        try {
            val snapshot = FirebaseFirestore.getInstance()
                .collection("evacuation_centers")
                .get()
                .await()
            
            snapshot.documents.forEach { doc ->
                val name = doc.getString("name") ?: return@forEach
                val place = doc.getGeoPoint("place") ?: return@forEach
                evacuationCenters.add(
                    EvacuationCenter(name, place.latitude, place.longitude)
                )
            }
        } catch (e: Exception) {
            // Handle error silently for offline mode
        }
    }
    
    // Calculate nearest center when home location or centers change
    // Includes both Firestore evacuation centers AND offline markers of type EVACUATION_CENTER
    LaunchedEffect(homeLocation, evacuationCenters.size, offlineMarkers.size, showOnlineShelters) {
        homeLocation?.let { home ->
            // Get Firestore evacuation centers with distance (optional)
            val firestoreCentersWithDistance = if (showOnlineShelters) {
                evacuationCenters.map { center ->
                    center.copy(distance = calculateDistance(
                        home.latitude, home.longitude,
                        center.latitude, center.longitude
                    ))
                }
            } else {
                emptyList()
            }
            
            // Get offline markers that are evacuation centers and convert to EvacuationCenter
            val offlineEvacCentersWithDistance = offlineMarkers
                .filter { it.category == MarkerCategory.EVACUATION_CENTER }
                .map { marker ->
                    EvacuationCenter(
                        name = marker.name,
                        latitude = marker.latitude,
                        longitude = marker.longitude,
                        distance = calculateDistance(
                            home.latitude, home.longitude,
                            marker.latitude, marker.longitude
                        )
                    )
                }
            
            // Combine both lists and find the nearest
            val allCenters = firestoreCentersWithDistance + offlineEvacCentersWithDistance
            nearestCenter = allCenters.minByOrNull { it.distance ?: Double.MAX_VALUE }
        }
    }
    
    // Prepare markers for display - filter by category AND proximity to current map center
    val markersToDisplay = offlineMarkers.filter { marker ->
        selectedCategories.contains(marker.category) &&
        calculateDistance(
            currentMapCenter.first, currentMapCenter.second,
            marker.latitude, marker.longitude
        ) <= MARKER_VISIBILITY_RADIUS_KM
    }
    // Force read of home location to ensure update
    val currentHomeLocation = homeLocation
    
    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setBuiltInZoomControls(false)
                    setMultiTouchControls(true)

                    controller.setZoom(14.0)
                    controller.setCenter(GeoPoint(cityCenter.first, cityCenter.second))
                    
                    // Add map listener to track current center
                    addMapListener(object : org.osmdroid.events.MapListener {
                        override fun onScroll(event: org.osmdroid.events.ScrollEvent?): Boolean {
                            val center = mapCenter
                            currentMapCenter = Pair(center.latitude, center.longitude)
                            return true
                        }
                        override fun onZoom(event: org.osmdroid.events.ZoomEvent?): Boolean = true
                    })

                    mapView = this
                }
            },
            update = { map ->
                val overlaysToKeep = map.overlays.filterIsInstance<org.osmdroid.views.overlay.TilesOverlay>()
                map.overlays.clear()
                map.overlays.addAll(overlaysToKeep)

                if (showOnlineShelters) {
                    evacuationCenters.forEach { center ->
                        val marker = Marker(map).apply {
                            position = GeoPoint(center.latitude, center.longitude)
                            title = center.name
                            snippet = center.distance?.let { "Distance: ${String.format("%.2f", it)} km" } ?: ""
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            icon = createMarkerIcon(context, MarkerCategory.EVACUATION_CENTER, false)
                        }
                        map.overlays.add(marker)
                    }
                }

                markersToDisplay.forEach { offlineMarker ->
                    val mapMarker = Marker(map).apply {
                        position = GeoPoint(offlineMarker.latitude, offlineMarker.longitude)
                        title = offlineMarker.name
                        snippet = "Tap to edit or delete"
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        icon = createMarkerIcon(context, offlineMarker.category, false)
                        
                        // Add click listener for edit/delete - only for admins
                        setOnMarkerClickListener { _, _ ->
                            if (userRole == "admin") {
                                selectedMarkerForEdit = offlineMarker
                                showEditMarkerSheet = true
                            }
                            true
                        }
                    }
                    map.overlays.add(mapMarker)
                }

                currentHomeLocation?.let { home ->
                    val homeMarker = Marker(map).apply {
                        position = GeoPoint(home.latitude, home.longitude)
                        title = home.name
                        snippet = if (home.isHouse) "Your Home" else "Saved Location"
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        icon = createHomeMarkerIcon(context)
                    }
                    map.overlays.add(homeMarker)
                }

                if (isSelectingLocation) {
                    map.setOnTouchListener { _, event ->
                        if (event.action == android.view.MotionEvent.ACTION_UP) {
                            val projection = map.projection
                            val geoPoint = projection.fromPixels(event.x.toInt(), event.y.toInt()) as GeoPoint

                            selectedPoint = geoPoint
                            isSelectingLocation = false

                            when (selectionMode) {
                                "home" -> showSetHomeDialog = true
                                "marker" -> showAddMarkerSheet = true
                            }
                        }
                        true
                    }
                } else {
                    map.setOnTouchListener(null)
                }

                map.postInvalidate()
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Filter chips at top
        AnimatedVisibility(
            visible = showFilters,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.95f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Filter Markers",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { showFilters = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close filters")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = showOnlineShelters,
                            onClick = { showOnlineShelters = !showOnlineShelters },
                            label = {
                                Text(
                                    "Online Shelters",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )

                        MarkerCategory.entries.forEach { category ->
                            val isSelected = selectedCategories.contains(category)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (isSelected) {
                                        selectedCategories.remove(category)
                                    } else {
                                        selectedCategories.add(category)
                                    }
                                },
                                label = { 
                                    Text(
                                        category.name.replace("_", " "),
                                        style = MaterialTheme.typography.bodySmall
                                    ) 
                                },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(getCategoryColor(category))
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }

                    if (userRole == "admin") {
                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { showPurgeSheltersDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Purge Online Shelters")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Marker counts
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${offlineMarkers.filter { selectedCategories.contains(it.category) }.size} of ${offlineMarkers.size} markers shown",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        // Selection mode indicator
        AnimatedVisibility(
            visible = isSelectingLocation,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Tap on the map to select location",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    IconButton(onClick = { 
                        isSelectingLocation = false
                        selectionMode = null
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                }
            }
        }
        
        // Nearest evacuation center card (Now at Top for better organization)
        AnimatedVisibility(
            visible = nearestCenter != null && !isSelectingLocation && !showFilters,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp)
                .zIndex(1f) // Ensure it stays above other overlays
        ) {
            nearestCenter?.let { center ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .clickable {
                            mapView?.controller?.animateTo(
                                GeoPoint(center.latitude, center.longitude)
                            )
                        },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "NEAREST SHELTER",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                            
                            Text(
                                text = center.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            
                            center.distance?.let {
                                Text(
                                    text = String.format("%.2f km away", it),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        
                        IconButton(
                            onClick = {
                                mapView?.controller?.animateTo(GeoPoint(center.latitude, center.longitude), 15.0, 1000L)
                            },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                .size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.MyLocation,
                                contentDescription = "Locate",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
        
        // Modern FAB Menu
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.End
        ) {
            // Filter button
            val filterScale by animateFloatAsState(
                targetValue = if (showFilters) 1.1f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            
            SmallFloatingActionButton(
                onClick = { showFilters = !showFilters },
                containerColor = if (showFilters) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (showFilters) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 8.dp
                ),
                modifier = Modifier.scale(filterScale)
            ) {
                Icon(Icons.Default.FilterList, contentDescription = "Filter markers")
            }
            
            // Recenter button
            SmallFloatingActionButton(
                onClick = {
                    if (homeLocation != null) {
                        mapView?.controller?.animateTo(
                            GeoPoint(homeLocation!!.latitude, homeLocation!!.longitude),
                            15.0,
                            1000L
                        )
                    } else {
                        shouldRecenterMap = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 8.dp
                )
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Recenter map")
            }

            // Nearest shelter recenter button
            if (nearestCenter != null) {
                SmallFloatingActionButton(
                    onClick = {
                        nearestCenter?.let { center ->
                            mapView?.controller?.animateTo(
                                GeoPoint(center.latitude, center.longitude),
                                15.0,
                                1000L
                            )
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 8.dp
                    )
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = "Nearest shelter")
                }
            }
            
            // Extended FAB for actions
            AnimatedVisibility(
                visible = !showExtendedFab && !isSelectingLocation,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                FloatingActionButton(
                    onClick = { showExtendedFab = true },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 8.dp,
                        pressedElevation = 12.dp
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = "More actions")
                }
            }
            
            // Expanded actions
            AnimatedVisibility(
                visible = showExtendedFab,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    // Set Home FAB
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 3.dp
                        ) {
                            Text(
                                text = "Set Home",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        FloatingActionButton(
                            onClick = {
                                selectionMode = "home"
                                isSelectingLocation = true
                                showExtendedFab = false
                            },
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            elevation = FloatingActionButtonDefaults.elevation(
                                defaultElevation = 6.dp
                            )
                        ) {
                            Icon(Icons.Default.Home, contentDescription = "Set Home")
                        }
                    }
                    
                    // Add Marker FAB - only for admins
                    if (userRole == "admin") {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                tonalElevation = 3.dp
                            ) {
                                Text(
                                    text = "Add Marker",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            FloatingActionButton(
                                onClick = {
                                    selectionMode = "marker"
                                    isSelectingLocation = true
                                    showExtendedFab = false
                                },
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                elevation = FloatingActionButtonDefaults.elevation(
                                    defaultElevation = 6.dp
                                )
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Marker")
                            }
                        }
                    }
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 3.dp
                        ) {
                            Text(
                                text = "Set Weather Location",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        FloatingActionButton(
                            onClick = {
                                showSetWeatherLocationDialog = true
                                showExtendedFab = false
                            },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            elevation = FloatingActionButtonDefaults.elevation(
                                defaultElevation = 6.dp
                            )
                        ) {
                            Icon(Icons.Default.WbSunny, contentDescription = "Set Weather Location")
                        }
                    }
                    
                    // Close button
                    SmallFloatingActionButton(
                        onClick = { showExtendedFab = false },
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close menu")
                    }
                }
            }
        }
    }
    
    // Set Home Dialog
    if (showSetHomeDialog && selectedPoint != null) {
        AlertDialog(
            onDismissRequest = { showSetHomeDialog = false },
            title = { 
                Text(
                    "Set Home Location",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                ) 
            },
            text = { 
                Text(
                    "Is this your house?",
                    style = MaterialTheme.typography.bodyLarge
                ) 
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val home = HomeLocation(
                                latitude = selectedPoint!!.latitude,
                                longitude = selectedPoint!!.longitude,
                                isHouse = true,
                                name = "My Home"
                            )
                            homeLocationDao.deleteHomeLocation()
                            homeLocationDao.insertHomeLocation(home)
                            homeLocationTrigger++
                            showSetHomeDialog = false
                            selectedPoint = null
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Yes", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val home = HomeLocation(
                                latitude = selectedPoint!!.latitude,
                                longitude = selectedPoint!!.longitude,
                                isHouse = false,
                                name = "Saved Location"
                            )
                            homeLocationDao.deleteHomeLocation()
                            homeLocationDao.insertHomeLocation(home)
                            homeLocationTrigger++
                            showSetHomeDialog = false
                            selectedPoint = null
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("No, just save", fontWeight = FontWeight.SemiBold)
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
    
    if (showSetWeatherLocationDialog) {
        val weatherLocationLabel = listOfNotNull(
            userBarangay?.takeIf { it.isNotBlank() },
            userCity?.takeIf { it.isNotBlank() },
            userProvince?.takeIf { it.isNotBlank() }
        ).joinToString(", ")
        AlertDialog(
            onDismissRequest = { showSetWeatherLocationDialog = false },
            title = { 
                Text(
                    "Set Weather Location",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                ) 
            },
            text = { 
                Text(
                    "Use the current map center as your weather location? Weather data will update to show conditions for this area.",
                    style = MaterialTheme.typography.bodyLarge
                ) 
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val resolvedWeatherLabel = when {
                                weatherLocationLabel.isNotBlank() -> weatherLocationLabel
                                userLat != null && userLon != null -> "Current Location"
                                else -> selectedCity.name
                            }
                            userPreferences.setWeatherLocation(
                                currentMapCenter.first,
                                currentMapCenter.second,
                                resolvedWeatherLabel
                            )
                            showSetWeatherLocationDialog = false
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Set Location", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showSetWeatherLocationDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel", fontWeight = FontWeight.SemiBold)
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showPurgeSheltersDialog) {
        AlertDialog(
            onDismissRequest = { showPurgeSheltersDialog = false },
            title = {
                Text(
                    "Purge Online Shelters",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "This will permanently delete ALL online evacuation centers from the server. This cannot be undone.",
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                val snapshot = FirebaseFirestore.getInstance()
                                    .collection("evacuation_centers")
                                    .get()
                                    .await()
                                snapshot.documents.forEach { doc ->
                                    doc.reference.delete().await()
                                }
                                evacuationCenters.clear()
                                nearestCenter = null
                            } catch (_: Exception) {
                                // Ignore errors for now
                            } finally {
                                showPurgeSheltersDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Purge", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showPurgeSheltersDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel", fontWeight = FontWeight.SemiBold)
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
    
    // Add Marker Bottom Sheet
    if (showAddMarkerSheet && selectedPoint != null) {
        AddMarkerBottomSheet(
            point = selectedPoint!!,
            onDismiss = { 
                showAddMarkerSheet = false
                selectedPoint = null
            },
            onSave = { name, category, notes ->
                scope.launch {
                    val marker = OfflineMarker(
                        name = name,
                        latitude = selectedPoint!!.latitude,
                        longitude = selectedPoint!!.longitude,
                        category = category,
                        notes = notes
                    )
                    offlineMarkerDao.insertMarker(marker)
                    showAddMarkerSheet = false
                    selectedPoint = null
                }
            }
        )
    }
    
    // Edit Marker Bottom Sheet
    if (showEditMarkerSheet && selectedMarkerForEdit != null) {
        EditMarkerBottomSheet(
            marker = selectedMarkerForEdit!!,
            onDismiss = { 
                showEditMarkerSheet = false
                selectedMarkerForEdit = null
            },
            onUpdate = { name, category, notes ->
                scope.launch {
                    val updatedMarker = selectedMarkerForEdit!!.copy(
                        name = name,
                        category = category,
                        notes = notes
                    )
                    offlineMarkerDao.updateMarker(updatedMarker)
                    showEditMarkerSheet = false
                    selectedMarkerForEdit = null
                }
            },
            onDelete = {
                scope.launch {
                    offlineMarkerDao.deleteMarker(selectedMarkerForEdit!!)
                    showEditMarkerSheet = false
                    selectedMarkerForEdit = null
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMarkerBottomSheet(
    point: GeoPoint,
    onDismiss: () -> Unit,
    onSave: (String, MarkerCategory, String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(MarkerCategory.EVACUATION_CENTER) }
    var expanded by remember { mutableStateOf(false) }
    
    val sheetState = rememberModalBottomSheetState()
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Box {
            // Gradient background
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        )
                    )
            )
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Add Marker",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Mark important locations",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(getCategoryColor(selectedCategory).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(getCategoryColor(selectedCategory))
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedCategory.name.replace("_", " "),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(16.dp)
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        MarkerCategory.entries.forEach { category ->
                            DropdownMenuItem(
                                text = { 
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(getCategoryColor(category))
                                        )
                                        Text(category.name.replace("_", " "))
                                    }
                                },
                                onClick = {
                                    selectedCategory = category
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(16.dp)
                )
                
                Spacer(modifier = Modifier.height(28.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = { onSave(name, selectedCategory, notes.ifBlank { null }) },
                        enabled = name.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp
                        )
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditMarkerBottomSheet(
    marker: OfflineMarker,
    onDismiss: () -> Unit,
    onUpdate: (String, MarkerCategory, String?) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(marker.name) }
    var notes by remember { mutableStateOf(marker.notes ?: "") }
    var selectedCategory by remember { mutableStateOf(marker.category) }
    var expanded by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    
    val sheetState = rememberModalBottomSheetState()
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Box {
            // Gradient background
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        )
                    )
            )
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Edit Marker",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Update or delete this marker",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(getCategoryColor(selectedCategory).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(getCategoryColor(selectedCategory))
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedCategory.name.replace("_", " "),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(16.dp)
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        MarkerCategory.entries.forEach { category ->
                            DropdownMenuItem(
                                text = { 
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(getCategoryColor(category))
                                        )
                                        Text(category.name.replace("_", " "))
                                    }
                                },
                                onClick = {
                                    selectedCategory = category
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(16.dp)
                )
                
                Spacer(modifier = Modifier.height(28.dp))
                
                // Delete confirmation
                if (showDeleteConfirmation) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Delete this marker?",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "This action cannot be undone.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showDeleteConfirmation = false },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Cancel")
                                }
                                Button(
                                    onClick = onDelete,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Delete")
                                }
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showDeleteConfirmation = true },
                            modifier = Modifier.weight(0.4f),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Delete", fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = { onUpdate(name, selectedCategory, notes.ifBlank { null }) },
                            enabled = name.isNotBlank(),
                            modifier = Modifier.weight(0.6f),
                            shape = RoundedCornerShape(16.dp),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 4.dp
                            )
                        ) {
                            Text("Update", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

private fun getCategoryColor(category: MarkerCategory): Color {
    return when (category) {
        MarkerCategory.EVACUATION_CENTER -> Color(0xFF1976D2)  // Blue (shelter)
        MarkerCategory.FLOOD_ZONE -> Color(0xFFD32F2F)          // Red (danger)
        MarkerCategory.SAFE_AREA -> Color(0xFF2E7D32)           // Green (safe)
        MarkerCategory.RESOURCE_CENTER -> Color(0xFFF9A825)     // Amber (resources)
    }
}

private fun createMarkerIcon(context: Context, category: MarkerCategory, isSelected: Boolean): Drawable {
    val color = when (category) {
        MarkerCategory.EVACUATION_CENTER -> android.graphics.Color.parseColor("#1976D2") // Blue (shelter)
        MarkerCategory.FLOOD_ZONE -> android.graphics.Color.parseColor("#D32F2F")         // Red (danger)
        MarkerCategory.SAFE_AREA -> android.graphics.Color.parseColor("#2E7D32")          // Green (safe)
        MarkerCategory.RESOURCE_CENTER -> android.graphics.Color.parseColor("#F9A825")    // Amber (resources)
    }
    return createPinDrawable(context, color, if (isSelected) 1.2f else 1f)
}

private fun createHomeMarkerIcon(context: Context): Drawable {
    val homeColor = android.graphics.Color.parseColor("#3F51B5") // Indigo for home
    return createPinDrawable(context, homeColor, 1.1f)
}

private fun createPinDrawable(context: Context, color: Int, scale: Float): Drawable {
    val pinWidth = (48 * scale).toInt()
    val pinHeight = (64 * scale).toInt()
    
    val bitmap = android.graphics.Bitmap.createBitmap(pinWidth, pinHeight, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    
    // Draw pin body (teardrop shape)
    val path = android.graphics.Path()
    val centerX = pinWidth / 2f
    val circleRadius = pinWidth / 2.5f
    val circleY = circleRadius + 4
    
    // Create teardrop shape
    path.addCircle(centerX, circleY, circleRadius, android.graphics.Path.Direction.CW)
    
    // Draw the point at bottom
    val pointPath = android.graphics.Path()
    pointPath.moveTo(centerX - circleRadius * 0.6f, circleY + circleRadius * 0.5f)
    pointPath.lineTo(centerX, pinHeight.toFloat() - 4)
    pointPath.lineTo(centerX + circleRadius * 0.6f, circleY + circleRadius * 0.5f)
    pointPath.close()
    
    // Draw shadow
    paint.color = android.graphics.Color.argb(80, 0, 0, 0)
    canvas.save()
    canvas.translate(2f, 3f)
    canvas.drawPath(path, paint)
    canvas.drawPath(pointPath, paint)
    canvas.restore()
    
    // Draw main pin body
    paint.color = color
    paint.style = android.graphics.Paint.Style.FILL
    canvas.drawPath(path, paint)
    canvas.drawPath(pointPath, paint)
    
    // Draw border
    paint.color = android.graphics.Color.argb(100, 0, 0, 0)
    paint.style = android.graphics.Paint.Style.STROKE
    paint.strokeWidth = 2f
    canvas.drawPath(path, paint)
    canvas.drawPath(pointPath, paint)
    
    // Draw white center dot
    paint.color = android.graphics.Color.WHITE
    paint.style = android.graphics.Paint.Style.FILL
    canvas.drawCircle(centerX, circleY, circleRadius * 0.35f, paint)
    
    return BitmapDrawable(context.resources, bitmap)
}

private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val earthRadius = 6371.0 // km
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return earthRadius * c
}
