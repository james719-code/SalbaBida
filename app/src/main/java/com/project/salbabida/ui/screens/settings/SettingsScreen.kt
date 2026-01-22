package com.project.salbabida.ui.screens.settings

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider as Divider
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.project.salbabida.SalbaBidaApplication
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val app = SalbaBidaApplication.getInstance()
    val preferences = app.userPreferences
    val database = app.database
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    
    val isDarkTheme by preferences.isDarkTheme.collectAsState(initial = false)
    val useDynamicColors by preferences.useDynamicColors.collectAsState(initial = true)
    
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showDeleteMarkersDialog by remember { mutableStateOf(false) }
    var showUpdateLocationDialog by remember { mutableStateOf(false) }
    var isUpdatingLocation by remember { mutableStateOf(false) }
    var locationUpdateMessage by remember { mutableStateOf<String?>(null) }
    var hasPermission by remember { mutableStateOf(false) }
    
    var showManualAddressDialog by remember { mutableStateOf(false) }
    var barangay by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var province by remember { mutableStateOf("") }
    var manualAddressError by remember { mutableStateOf<String?>(null) }
    
    // Load existing address if any
    val savedBarangay by preferences.userBarangay.collectAsState(initial = "")
    val savedCity by preferences.userCity.collectAsState(initial = "")
    val savedProvince by preferences.userProvince.collectAsState(initial = "")
    
    LaunchedEffect(showManualAddressDialog) {
        if (showManualAddressDialog) {
            barangay = savedBarangay ?: ""
            city = savedCity ?: ""
            province = savedProvince ?: ""
        }
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (hasPermission) {
            showUpdateLocationDialog = true
        }
    }
    
    LaunchedEffect(Unit) {
        hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    fun updateLocation() {
        isUpdatingLocation = true
        locationUpdateMessage = null
        scope.launch {
            try {
                val cancellationTokenSource = CancellationTokenSource()
                val location = fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationTokenSource.token
                ).await()
                
                if (location != null) {
                    preferences.setUserLocation(location.latitude, location.longitude)
                    locationUpdateMessage = "Location updated successfully!"
                } else {
                    locationUpdateMessage = "Could not get location. Please try again."
                }
            } catch (e: Exception) {
                locationUpdateMessage = "Error: ${e.message}"
            } finally {
                isUpdatingLocation = false
            }
        }
    }

    fun updateManualAddress() {
        if (barangay.isBlank() || city.isBlank() || province.isBlank()) {
            manualAddressError = "All fields are required"
            return
        }
        
        isUpdatingLocation = true
        scope.launch {
            try {
                val fullAddress = "$barangay, $city, $province, Philippines"
                val geocoder = android.location.Geocoder(context)
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocationName(fullAddress, 1)
                
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    preferences.setUserLocation(address.latitude, address.longitude)
                }
                
                preferences.setUserAddress(barangay, city, province)
                showManualAddressDialog = false
            } catch (e: Exception) {
                // Save even if geocoding fails
                preferences.setUserAddress(barangay, city, province)
                showManualAddressDialog = false
            } finally {
                isUpdatingLocation = false
            }
        }
    }
    
    val colorScheme = MaterialTheme.colorScheme
    val backgroundColor = colorScheme.background
    val outlineColor = colorScheme.outline
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Settings", 
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Back",
                            tint = colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor,
                    titleContentColor = colorScheme.onBackground,
                    navigationIconContentColor = colorScheme.onBackground
                ),
                modifier = Modifier.drawBehind {
                    drawLine(
                        color = outlineColor.copy(alpha = 0.2f),
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Location Section
            SettingsSection(
                title = "Location",
                icon = Icons.Default.LocationOn
            ) {
                SettingsItem(
                    title = "Update Location",
                    description = "Refresh your current GPS location for accuracy",
                    icon = Icons.Default.MyLocation,
                    trailing = {
                        Button(
                            onClick = {
                                if (hasPermission) {
                                    showUpdateLocationDialog = true
                                } else {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                        ) {
                            Text("Update")
                        }
                    }
                )
                
                Divider(modifier = Modifier.padding(horizontal = 16.dp), color = outlineColor.copy(alpha = 0.05f))
                
                SettingsItem(
                    title = "Set Address Manually",
                    description = "Input your Barangay, City, and Province",
                    icon = Icons.Default.Map,
                    onClick = { showManualAddressDialog = true }
                )
            }
            
            // Appearance Section
            SettingsSection(
                title = "Appearance",
                icon = Icons.Default.Palette
            ) {
                SettingsItem(
                    title = "Dark Theme",
                    description = "Use a battery-saving dark color scheme",
                    icon = Icons.Default.DarkMode,
                    trailing = {
                        Switch(
                            checked = isDarkTheme,
                            onCheckedChange = { 
                                scope.launch { preferences.setDarkTheme(it) }
                            }
                        )
                    }
                )
                
                Divider(modifier = Modifier.padding(horizontal = 16.dp), color = outlineColor.copy(alpha = 0.05f))
                
                SettingsItem(
                    title = "Dynamic Colors",
                    description = "Adapt to system colors (Android 12+)",
                    icon = Icons.Default.FormatPaint,
                    trailing = {
                        Switch(
                            checked = useDynamicColors,
                            onCheckedChange = { 
                                scope.launch { preferences.setDynamicColors(it) }
                            }
                        )
                    }
                )
            }
            
            // Data Section
            SettingsSection(
                title = "Data Management",
                icon = Icons.Default.Storage
            ) {
                SettingsItem(
                    title = "Clear Cache",
                    description = "Remove temporary weather data",
                    icon = Icons.Default.DeleteSweep,
                    color = colorScheme.error,
                    onClick = { showClearCacheDialog = true }
                )
                
                Divider(modifier = Modifier.padding(horizontal = 16.dp), color = outlineColor.copy(alpha = 0.05f))
                
                SettingsItem(
                    title = "Delete All Markers",
                    description = "Permanently remove all map markers",
                    icon = Icons.Default.LayersClear,
                    color = colorScheme.error,
                    onClick = { showDeleteMarkersDialog = true }
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
    
    if (showManualAddressDialog) {
        AlertDialog(
            onDismissRequest = { if (!isUpdatingLocation) showManualAddressDialog = false },
            title = { Text("Manual Address") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    androidx.compose.material3.OutlinedTextField(
                        value = barangay,
                        onValueChange = { barangay = it; manualAddressError = null },
                        label = { Text("Barangay") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isUpdatingLocation
                    )
                    androidx.compose.material3.OutlinedTextField(
                        value = city,
                        onValueChange = { city = it; manualAddressError = null },
                        label = { Text("City/Municipality") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isUpdatingLocation
                    )
                    androidx.compose.material3.OutlinedTextField(
                        value = province,
                        onValueChange = { province = it; manualAddressError = null },
                        label = { Text("Province") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isUpdatingLocation
                    )
                    
                    if (manualAddressError != null) {
                        Text(manualAddressError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    
                    if (isUpdatingLocation) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { updateManualAddress() },
                    enabled = !isUpdatingLocation
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showManualAddressDialog = false },
                    enabled = !isUpdatingLocation
                ) {
                    Text("Cancel")
                }
            }
        )
    }
    
    if (showUpdateLocationDialog) {
        AlertDialog(
            onDismissRequest = { 
                if (!isUpdatingLocation) {
                    showUpdateLocationDialog = false
                    locationUpdateMessage = null
                }
            },
            title = { Text("Update Location") },
            text = { 
                if (isUpdatingLocation) {
                    Column {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Getting your location...")
                    }
                } else {
                    Text(locationUpdateMessage ?: "Update your location to get accurate evacuation center distances.")
                }
            },
            confirmButton = {
                if (!isUpdatingLocation && locationUpdateMessage == null) {
                    Button(onClick = { updateLocation() }) {
                        Text("Update Now")
                    }
                } else if (!isUpdatingLocation) {
                    Button(onClick = { 
                        showUpdateLocationDialog = false
                        locationUpdateMessage = null
                    }) {
                        Text("Done")
                    }
                }
            },
            dismissButton = {
                if (!isUpdatingLocation && locationUpdateMessage == null) {
                    TextButton(onClick = { showUpdateLocationDialog = false }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
    
    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text("Clear Cache") },
            text = { Text("This will delete all cached weather data. Continue?") },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        database.weatherCacheDao().clearAll()
                        showClearCacheDialog = false
                    }
                }) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    if (showDeleteMarkersDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteMarkersDialog = false },
            title = { Text("Delete All Markers") },
            text = { Text("This will permanently delete all your saved map markers. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            database.offlineMarkerDao().clearAll()
                            showDeleteMarkersDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteMarkersDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun SettingsItem(
    title: String,
    description: String,
    icon: ImageVector,
    color: Color = Color.Unspecified,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val resolvedColor = if (color == Color.Unspecified) MaterialTheme.colorScheme.onSurface else color
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = RoundedCornerShape(12.dp),
            color = if (resolvedColor == MaterialTheme.colorScheme.error) resolvedColor.copy(alpha = 0.1f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (resolvedColor == MaterialTheme.colorScheme.error) resolvedColor else MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(10.dp)
            )
        }
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = resolvedColor
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        if (trailing != null) {
            trailing()
        }
    }
}
