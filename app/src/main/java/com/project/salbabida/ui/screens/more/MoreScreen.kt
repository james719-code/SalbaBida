package com.project.salbabida.ui.screens.more

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.auth.FirebaseAuth
import com.project.salbabida.BuildConfig
import com.project.salbabida.R
import com.project.salbabida.SalbaBidaApplication
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
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
    
    val colorScheme = MaterialTheme.colorScheme
    val backgroundColor = colorScheme.background
    val outlineColor = colorScheme.outline
    val primaryColor = colorScheme.primary

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
                preferences.setUserAddress(barangay, city, province)
                showManualAddressDialog = false
            } finally {
                isUpdatingLocation = false
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        // App Identity Header
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(120.dp)
        ) {
            Surface(
                modifier = Modifier.size(100.dp),
                shape = RoundedCornerShape(28.dp),
                color = colorScheme.primary.copy(alpha = 0.1f)
            ) {}
            
            Surface(
                modifier = Modifier.size(80.dp),
                shape = RoundedCornerShape(24.dp),
                color = colorScheme.primary,
                tonalElevation = 4.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(colorScheme.primary, colorScheme.primary.copy(alpha = 0.8f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.mipmap.ic_launcher),
                        contentDescription = "App Logo",
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "SALBA-bida",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = colorScheme.onSurface
        )
        
        Surface(
            color = colorScheme.primary.copy(alpha = 0.1f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "Version ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.labelMedium,
                color = colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                fontWeight = FontWeight.SemiBold
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Settings Sections
        SettingsSection(
            title = "Location Settings",
            icon = Icons.Default.LocationOn
        ) {
            SettingsItem(
                title = "Update GPS Location",
                description = "Keep your evacuation distance accurate",
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
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Update", style = MaterialTheme.typography.labelLarge)
                    }
                }
            )
            
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = outlineColor.copy(alpha = 0.05f))
            
            SettingsItem(
                title = "Set Address Manually",
                description = "Input Barangay, City, and Province",
                icon = Icons.Default.Map,
                onClick = { showManualAddressDialog = true }
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        SettingsSection(
            title = "Appearance",
            icon = Icons.Default.Palette
        ) {
            SettingsItem(
                title = "Dark Mode",
                description = "Toggle dark/light theme",
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
            
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = outlineColor.copy(alpha = 0.05f))
            
            SettingsItem(
                title = "Dynamic Colors",
                description = "Material You integration",
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
        
        Spacer(modifier = Modifier.height(24.dp))
        
        SettingsSection(
            title = "Account & Data",
            icon = Icons.Default.ManageAccounts
        ) {
            SettingsItem(
                title = "Logout",
                description = "Sign out of your account",
                icon = Icons.Default.Logout,
                color = colorScheme.error,
                onClick = {
                    FirebaseAuth.getInstance().signOut()
                    onLogout()
                }
            )
            
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = outlineColor.copy(alpha = 0.05f))
            
            SettingsItem(
                title = "Clear Cache",
                description = "Reset weather & local files",
                icon = Icons.Default.DeleteSweep,
                onClick = { showClearCacheDialog = true }
            )
            
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = outlineColor.copy(alpha = 0.05f))
            
            SettingsItem(
                title = "Reset Markers",
                description = "Delete all saved map points",
                icon = Icons.Default.LayersClear,
                color = colorScheme.error,
                onClick = { showDeleteMarkersDialog = true }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        // Developer / Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = colorScheme.primary,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Developer",
                            style = MaterialTheme.typography.labelMedium,
                            color = colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "James Ryan S. Gallego",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "A flood disaster management tool designed for the Philippines.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Locating...")
                    }
                } else {
                    Text(locationUpdateMessage ?: "Access your current coordinates for precision mapping.")
                }
            },
            confirmButton = {
                if (!isUpdatingLocation && locationUpdateMessage == null) {
                    Button(onClick = { updateLocation() }) { Text("Locate Now") }
                } else if (!isUpdatingLocation) {
                    Button(onClick = { 
                        showUpdateLocationDialog = false
                        locationUpdateMessage = null
                    }) { Text("Close") }
                }
            }
        )
    }
    
    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text("Clear Cache") },
            text = { Text("Delete all cached weather records?") },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        database.weatherCacheDao().clearAll()
                        showClearCacheDialog = false
                    }
                }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) { Text("Cancel") }
            }
        )
    }
    
    if (showDeleteMarkersDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteMarkersDialog = false },
            title = { Text("Reset Markers") },
            text = { Text("Permanently delete ALL saved map markers?") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            database.offlineMarkerDao().clearAll()
                            showDeleteMarkersDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.error)
                ) { Text("Reset All") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteMarkersDialog = false }) { Text("Cancel") }
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
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
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
