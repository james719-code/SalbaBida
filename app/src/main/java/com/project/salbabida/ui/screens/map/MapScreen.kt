package com.project.salbabida.ui.screens.map

import android.content.Context
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.project.salbabida.data.database.entities.MarkerCategory
import com.project.salbabida.data.database.entities.OfflineMarker
import com.project.salbabida.data.repository.EvacuationCenter
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

// ────────────────────────────────────────────────────────────────────────────
// MapScreen — delegates all state & IO to MapViewModel (no !! assertions)
// ────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MapScreen(modifier: Modifier = Modifier) {
    val viewModel: MapViewModel = hiltViewModel()

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val filteredMarkers by viewModel.filteredMarkers.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var mapView by remember { mutableStateOf<MapView?>(null) }

    // Recenter map when the ViewModel requests it
    LaunchedEffect(state.shouldRecenterMap, mapView) {
        val map = mapView ?: return@LaunchedEffect
        if (!state.shouldRecenterMap) return@LaunchedEffect
        val center = GeoPoint(state.cityCenter.first, state.cityCenter.second)
        if (state.isFirstLoad) {
            map.controller.setCenter(center)
            map.controller.setZoom(14.0)
        } else {
            map.controller.animateTo(center, 18.0, 1000L)
        }
        viewModel.onMapRecentered()
    }

    Box(modifier = modifier) {
        // ── Map ────────────────────────────────────────────────────────
        MapOsmView(
            context = context,
            cityCenter = state.cityCenter,
            evacuationCenters = if (state.showOnlineShelters) state.evacuationCenters else emptyList(),
            filteredMarkers = filteredMarkers,
            homeLocation = state.homeLocation?.let {
                Triple(it.latitude, it.longitude, it.name)
            },
            isSelectingLocation = state.isSelectingLocation,
            userRole = state.userRole,
            onMapCreated = { mapView = it },
            onMapCenterChanged = { lat, lon -> viewModel.onMapCenterChanged(lat, lon) },
            onLocationTapped = { lat, lon -> viewModel.onLocationSelected(lat, lon) },
            onMarkerClicked = { marker -> viewModel.selectMarkerForEdit(marker) }
        )

        // ── Filter overlay ─────────────────────────────────────────────
        FilterOverlay(
            visible = state.showFilters,
            selectedCategories = state.selectedCategories,
            showOnlineShelters = state.showOnlineShelters,
            userRole = state.userRole,
            totalMarkerCount = state.offlineMarkers.size,
            filteredMarkerCount = state.offlineMarkers.count { state.selectedCategories.contains(it.category) },
            onClose = { viewModel.toggleFilters() },
            onToggleOnlineShelters = { viewModel.toggleOnlineShelters() },
            onToggleCategory = { viewModel.toggleCategory(it) },
            onPurgeShelters = { viewModel.showPurgeSheltersDialog(true) },
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp)
        )

        // ── "Tap to select" banner ─────────────────────────────────────
        SelectionBanner(
            visible = state.isSelectingLocation,
            onCancel = { viewModel.cancelSelection() },
            modifier = Modifier.align(Alignment.TopCenter).padding(16.dp)
        )

        // ── Nearest shelter card ───────────────────────────────────────
        NearestShelterCard(
            center = state.nearestCenter,
            visible = state.nearestCenter != null && !state.isSelectingLocation && !state.showFilters,
            onNavigate = { center ->
                mapView?.controller?.animateTo(
                    GeoPoint(center.latitude, center.longitude), 15.0, 1000L
                )
            },
            modifier = Modifier.align(Alignment.TopCenter).padding(16.dp).zIndex(1f)
        )

        // ── FAB column ─────────────────────────────────────────────────
        MapFabColumn(
            state = state,
            onToggleFilters = { viewModel.toggleFilters() },
            onRecenter = { viewModel.recenterToHome() },
            onNearestShelter = { viewModel.recenterToNearestShelter() },
            onExpandFab = { viewModel.showExtendedFab(true) },
            onCollapseFab = { viewModel.showExtendedFab(false) },
            onSetHome = { viewModel.startSelectingLocation("home") },
            onAddMarker = { viewModel.startSelectingLocation("marker") },
            onSetWeatherLocation = {
                viewModel.showSetWeatherLocationDialog(true)
                viewModel.showExtendedFab(false)
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        )
    }

    // ── Dialogs & sheets ───────────────────────────────────────────────
    if (state.showSetHomeDialog && state.selectedPoint != null) {
        SetHomeDialog(
            onConfirm = { isHouse -> viewModel.saveHomeLocation(isHouse) },
            onDismiss = { viewModel.dismissSetHomeDialog() }
        )
    }

    if (state.showSetWeatherLocationDialog) {
        SetWeatherLocationDialog(
            onConfirm = { viewModel.setWeatherLocation() },
            onDismiss = { viewModel.showSetWeatherLocationDialog(false) }
        )
    }

    if (state.showPurgeSheltersDialog) {
        PurgeSheltersDialog(
            isPurging = state.isPurging,
            onConfirm = { viewModel.purgeOnlineShelters() },
            onDismiss = { viewModel.showPurgeSheltersDialog(false) }
        )
    }

    val addSheetPoint = state.selectedPoint
    if (state.showAddMarkerSheet && addSheetPoint != null) {
        AddMarkerBottomSheet(
            point = GeoPoint(addSheetPoint.first, addSheetPoint.second),
            onDismiss = { viewModel.dismissAddMarkerSheet() },
            onSave = { name, category, notes -> viewModel.saveMarker(name, category, notes) }
        )
    }

    val editMarker = state.selectedMarkerForEdit
    if (state.showEditMarkerSheet && editMarker != null) {
        EditMarkerBottomSheet(
            marker = editMarker,
            onDismiss = { viewModel.dismissEditMarkerSheet() },
            onUpdate = { name, cat, notes -> viewModel.updateMarker(name, cat, notes) },
            onDelete = { viewModel.deleteSelectedMarker() }
        )
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Sub-composables — each small & focused
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun MapOsmView(
    context: Context,
    cityCenter: Pair<Double, Double>,
    evacuationCenters: List<EvacuationCenter>,
    filteredMarkers: List<OfflineMarker>,
    homeLocation: Triple<Double, Double, String>?,
    isSelectingLocation: Boolean,
    userRole: String?,
    onMapCreated: (MapView) -> Unit,
    onMapCenterChanged: (Double, Double) -> Unit,
    onLocationTapped: (Double, Double) -> Unit,
    onMarkerClicked: (OfflineMarker) -> Unit
) {
    // Track MapView for lifecycle management
    var localMapView by remember { mutableStateOf<MapView?>(null) }

    // Lifecycle: onResume / onPause / onDetach
    DisposableEffect(localMapView) {
        localMapView?.onResume()
        onDispose {
            localMapView?.onPause()
            localMapView?.onDetach()
        }
    }

    AndroidView(
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setBuiltInZoomControls(false)
                setMultiTouchControls(true)
                controller.setZoom(14.0)
                controller.setCenter(GeoPoint(cityCenter.first, cityCenter.second))

                // Add GPS location overlay (blue dot)
                val locationOverlay = org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay(
                    org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider(ctx), this
                )
                locationOverlay.enableMyLocation()
                overlays.add(locationOverlay)

                addMapListener(object : org.osmdroid.events.MapListener {
                    override fun onScroll(event: org.osmdroid.events.ScrollEvent?): Boolean {
                        val c = mapCenter
                        onMapCenterChanged(c.latitude, c.longitude)
                        return true
                    }
                    override fun onZoom(event: org.osmdroid.events.ZoomEvent?): Boolean = true
                })
                localMapView = this
                onMapCreated(this)
            }
        },
        update = { map ->
            // Keep tile overlays and GPS location overlay, remove everything else
            val overlaysToKeep = map.overlays.filter { overlay ->
                overlay is org.osmdroid.views.overlay.TilesOverlay ||
                overlay is org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
            }
            map.overlays.clear()
            map.overlays.addAll(overlaysToKeep)

            // Firestore evacuation centers
            evacuationCenters.forEach { center ->
                val marker = Marker(map).apply {
                    position = GeoPoint(center.latitude, center.longitude)
                    title = center.name
                    snippet = center.distance?.let { "Distance: ${String.format("%.2f", it)} km" }.orEmpty()
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    icon = MarkerIconCache.getMarkerIcon(context, MarkerCategory.EVACUATION_CENTER, false)
                }
                map.overlays.add(marker)
            }

            // Offline markers
            filteredMarkers.forEach { offlineMarker ->
                val mapMarker = Marker(map).apply {
                    position = GeoPoint(offlineMarker.latitude, offlineMarker.longitude)
                    title = offlineMarker.name
                    snippet = "Tap to edit or delete"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    icon = MarkerIconCache.getMarkerIcon(context, offlineMarker.category, false)
                    setOnMarkerClickListener { _, _ ->
                        if (userRole == "admin") {
                            onMarkerClicked(offlineMarker)
                        }
                        true
                    }
                }
                map.overlays.add(mapMarker)
            }

            // Home marker
            homeLocation?.let { (lat, lon, name) ->
                val homeMarker = Marker(map).apply {
                    position = GeoPoint(lat, lon)
                    title = name
                    snippet = "Your Home"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    icon = MarkerIconCache.getHomeIcon(context)
                }
                map.overlays.add(homeMarker)
            }

            // Selection tap handler
            if (isSelectingLocation) {
                map.setOnTouchListener { _, event ->
                    if (event.action == android.view.MotionEvent.ACTION_UP) {
                        val geoPoint = map.projection.fromPixels(
                            event.x.toInt(), event.y.toInt()
                        ) as GeoPoint
                        onLocationTapped(geoPoint.latitude, geoPoint.longitude)
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
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterOverlay(
    visible: Boolean,
    selectedCategories: Set<MarkerCategory>,
    showOnlineShelters: Boolean,
    userRole: String?,
    totalMarkerCount: Int,
    filteredMarkerCount: Int,
    onClose: () -> Unit,
    onToggleOnlineShelters: () -> Unit,
    onToggleCategory: (MarkerCategory) -> Unit,
    onPurgeShelters: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.95f),
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
                    Text("Filter Markers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onClose) {
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
                        onClick = onToggleOnlineShelters,
                        label = { Text("Online Shelters", style = MaterialTheme.typography.bodySmall) },
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
                        FilterChip(
                            selected = selectedCategories.contains(category),
                            onClick = { onToggleCategory(category) },
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
                        onClick = onPurgeShelters,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Purge Online Shelters") }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$filteredMarkerCount of $totalMarkerCount markers shown",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SelectionBanner(
    visible: Boolean,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn() + fadeIn(),
        exit = scaleOut() + fadeOut(),
        modifier = modifier
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
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
                    "Tap on the map to select location",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                IconButton(onClick = onCancel) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel")
                }
            }
        }
    }
}

@Composable
private fun NearestShelterCard(
    center: EvacuationCenter?,
    visible: Boolean,
    onNavigate: (EvacuationCenter) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        center?.let { c ->
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .clickable { onNavigate(c) },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                            "NEAREST SHELTER",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            c.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        c.distance?.let {
                            Text(
                                String.format("%.2f km away", it),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(
                        onClick = { onNavigate(c) },
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                RoundedCornerShape(12.dp)
                            )
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
}

@Composable
private fun MapFabColumn(
    state: MapUiState,
    onToggleFilters: () -> Unit,
    onRecenter: () -> Unit,
    onNearestShelter: () -> Unit,
    onExpandFab: () -> Unit,
    onCollapseFab: () -> Unit,
    onSetHome: () -> Unit,
    onAddMarker: () -> Unit,
    onSetWeatherLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.End
    ) {
        // Filter
        val filterScale by animateFloatAsState(
            targetValue = if (state.showFilters) 1.1f else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        SmallFloatingActionButton(
            onClick = onToggleFilters,
            containerColor = if (state.showFilters) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (state.showFilters) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 6.dp,
                pressedElevation = 8.dp
            ),
            modifier = Modifier.scale(filterScale)
        ) { Icon(Icons.Default.FilterList, contentDescription = "Filter markers") }

        // Recenter
        SmallFloatingActionButton(
            onClick = onRecenter,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 6.dp,
                pressedElevation = 8.dp
            )
        ) { Icon(Icons.Default.MyLocation, contentDescription = "Recenter map") }

        // Nearest shelter
        if (state.nearestCenter != null) {
            SmallFloatingActionButton(
                onClick = onNearestShelter,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 8.dp
                )
            ) { Icon(Icons.Default.LocationOn, contentDescription = "Nearest shelter") }
        }

        // Expand FAB
        AnimatedVisibility(
            visible = !state.showExtendedFab && !state.isSelectingLocation,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            FloatingActionButton(
                onClick = onExpandFab,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 12.dp
                )
            ) { Icon(Icons.Default.Add, contentDescription = "More actions") }
        }

        // Expanded actions
        AnimatedVisibility(
            visible = state.showExtendedFab,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.End
            ) {
                FabAction(
                    label = "Set Home",
                    icon = { Icon(Icons.Default.Home, contentDescription = "Set Home") },
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    onClick = onSetHome
                )

                if (state.userRole == "admin") {
                    FabAction(
                        label = "Add Marker",
                        icon = { Icon(Icons.Default.Add, contentDescription = "Add Marker") },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        onClick = onAddMarker
                    )
                }

                FabAction(
                    label = "Set Weather Location",
                    icon = {
                        Icon(
                            Icons.Default.WbSunny,
                            contentDescription = "Set Weather Location"
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    onClick = onSetWeatherLocation
                )

                SmallFloatingActionButton(
                    onClick = onCollapseFab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ) { Icon(Icons.Default.Close, contentDescription = "Close menu") }
            }
        }
    }
}

@Composable
private fun FabAction(
    label: String,
    icon: @Composable () -> Unit,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
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
                label,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
        FloatingActionButton(
            onClick = onClick,
            containerColor = containerColor,
            contentColor = contentColor,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
        ) { icon() }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Dialogs
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun SetHomeDialog(onConfirm: (Boolean) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Set Home Location",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text("Is this your house?", style = MaterialTheme.typography.bodyLarge)
        },
        confirmButton = {
            Button(onClick = { onConfirm(true) }, shape = RoundedCornerShape(12.dp)) {
                Text("Yes", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = { onConfirm(false) }, shape = RoundedCornerShape(12.dp)) {
                Text("No, just save", fontWeight = FontWeight.SemiBold)
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun SetWeatherLocationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
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
            Button(onClick = onConfirm, shape = RoundedCornerShape(12.dp)) {
                Text("Set Location", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
                Text("Cancel", fontWeight = FontWeight.SemiBold)
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun PurgeSheltersDialog(
    isPurging: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isPurging) onDismiss() },
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
                onClick = onConfirm,
                enabled = !isPurging,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    if (isPurging) "Purging\u2026" else "Purge",
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isPurging,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel", fontWeight = FontWeight.SemiBold)
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

// ────────────────────────────────────────────────────────────────────────────
// Bottom sheets  (Add / Edit marker)
// ────────────────────────────────────────────────────────────────────────────

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
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
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
                    Modifier.fillMaxWidth(),
                    Arrangement.SpaceBetween,
                    Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Add Marker",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Mark important locations",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(getCategoryColor(selectedCategory).copy(alpha = 0.2f)),
                        Alignment.Center
                    ) {
                        Box(
                            Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(getCategoryColor(selectedCategory))
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )
                Spacer(Modifier.height(16.dp))
                CategoryDropdown(selectedCategory, expanded, { expanded = it }) {
                    selectedCategory = it; expanded = false
                }
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(16.dp)
                )
                Spacer(Modifier.height(28.dp))
                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) { Text("Cancel", fontWeight = FontWeight.SemiBold) }
                    Button(
                        onClick = {
                            onSave(
                                name,
                                selectedCategory,
                                notes.ifBlank { null })
                        },
                        enabled = name.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) { Text("Save", fontWeight = FontWeight.Bold) }
                }
                Spacer(Modifier.height(16.dp))
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
    var notes by remember { mutableStateOf(marker.notes.orEmpty()) }
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
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
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
                    Modifier.fillMaxWidth(),
                    Arrangement.SpaceBetween,
                    Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Edit Marker",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Update or delete this marker",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(getCategoryColor(selectedCategory).copy(alpha = 0.2f)),
                        Alignment.Center
                    ) {
                        Box(
                            Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(getCategoryColor(selectedCategory))
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )
                Spacer(Modifier.height(16.dp))
                CategoryDropdown(selectedCategory, expanded, { expanded = it }) {
                    selectedCategory = it; expanded = false
                }
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(16.dp)
                )
                Spacer(Modifier.height(28.dp))

                if (showDeleteConfirmation) {
                    Card(
                        Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                "Delete this marker?",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "This action cannot be undone.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(Modifier.height(16.dp))
                            Row(
                                Modifier.fillMaxWidth(),
                                Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showDeleteConfirmation = false },
                                    Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) { Text("Cancel") }
                                Button(
                                    onClick = onDelete,
                                    Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) { Text("Delete") }
                            }
                        }
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = { showDeleteConfirmation = true },
                            Modifier.weight(0.4f),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) { Text("Delete", fontWeight = FontWeight.SemiBold) }
                        Button(
                            onClick = {
                                onUpdate(
                                    name,
                                    selectedCategory,
                                    notes.ifBlank { null })
                            },
                            enabled = name.isNotBlank(),
                            modifier = Modifier.weight(0.6f),
                            shape = RoundedCornerShape(16.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) { Text("Update", fontWeight = FontWeight.Bold) }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    selected: MarkerCategory,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (MarkerCategory) -> Unit
) {
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = onExpandedChange) {
        OutlinedTextField(
            value = selected.name.replace("_", " "),
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
            onDismissRequest = { onExpandedChange(false) }
        ) {
            MarkerCategory.entries.forEach { category ->
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(getCategoryColor(category))
                            )
                            Text(category.name.replace("_", " "))
                        }
                    },
                    onClick = { onSelect(category) }
                )
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Drawing helpers (unchanged logic)
// ────────────────────────────────────────────────────────────────────────────

private fun getCategoryColor(category: MarkerCategory): Color = when (category) {
    MarkerCategory.EVACUATION_CENTER -> Color(0xFF1976D2)
    MarkerCategory.FLOOD_ZONE -> Color(0xFFD32F2F)
    MarkerCategory.SAFE_AREA -> Color(0xFF2E7D32)
    MarkerCategory.RESOURCE_CENTER -> Color(0xFFF9A825)
}
