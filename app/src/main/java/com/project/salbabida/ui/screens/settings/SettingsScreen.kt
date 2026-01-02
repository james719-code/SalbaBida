package com.project.salbabida.ui.screens.settings

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Location",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                ListItem(
                    headlineContent = { Text("Update Location") },
                    supportingContent = { Text("Refresh your current GPS location") },
                    leadingContent = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                    trailingContent = {
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
                            }
                        ) {
                            Text("Update")
                        }
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Appearance",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                ListItem(
                    headlineContent = { Text("Dark Theme") },
                    supportingContent = { Text("Use dark color scheme") },
                    leadingContent = { Icon(Icons.Default.DarkMode, contentDescription = null) },
                    trailingContent = {
                        Switch(
                            checked = isDarkTheme,
                            onCheckedChange = { 
                                scope.launch { preferences.setDarkTheme(it) }
                            }
                        )
                    }
                )
                
                ListItem(
                    headlineContent = { Text("Dynamic Colors") },
                    supportingContent = { Text("Use system accent colors (Android 12+)") },
                    leadingContent = { Icon(Icons.Default.Palette, contentDescription = null) },
                    trailingContent = {
                        Switch(
                            checked = useDynamicColors,
                            onCheckedChange = { 
                                scope.launch { preferences.setDynamicColors(it) }
                            }
                        )
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Data",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                ListItem(
                    headlineContent = { Text("Clear Cache") },
                    supportingContent = { Text("Delete cached weather data") },
                    leadingContent = { 
                        Icon(
                            Icons.Default.Delete, 
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    trailingContent = {
                        TextButton(onClick = { showClearCacheDialog = true }) {
                            Text("Clear")
                        }
                    }
                )
                
                ListItem(
                    headlineContent = { Text("Delete All Markers") },
                    supportingContent = { Text("Remove all saved map markers") },
                    leadingContent = { 
                        Icon(
                            Icons.Default.Delete, 
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    trailingContent = {
                        TextButton(onClick = { showDeleteMarkersDialog = true }) {
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        }
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
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
