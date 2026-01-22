package com.project.salbabida.ui.screens.auth

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.project.salbabida.R
import com.project.salbabida.SalbaBidaApplication
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    onNavigateBack: () -> Unit,
    onSignUpSuccess: () -> Unit
) {
    val context = LocalContext.current
    val app = SalbaBidaApplication.getInstance()
    val preferences = app.userPreferences
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isCapturingLocation by remember { mutableStateOf(false) }
    var hasPermission by remember { mutableStateOf(false) }
    var pendingPermissionSignUp by remember { mutableStateOf(false) }
    var shouldLaunchPermission by remember { mutableStateOf(false) }
    
    var useGPS by remember { mutableStateOf(true) }
    var barangay by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var province by remember { mutableStateOf("") }
    
    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }
    var addressError by remember { mutableStateOf<String?>(null) }
    var locationError by remember { mutableStateOf<String?>(null) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    
    // Check for existing permissions on launch
    LaunchedEffect(Unit) {
        hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    data class ResolvedLocation(
        val latitude: Double,
        val longitude: Double,
        val barangay: String? = null,
        val city: String? = null,
        val province: String? = null
    )

    // Function to resolve location before creating account
    suspend fun resolveLocation(): ResolvedLocation? {
        isCapturingLocation = true
        return try {
            if (useGPS) {
                val cancellationTokenSource = CancellationTokenSource()
                val location = fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationTokenSource.token
                ).await()

                if (location == null) {
                    locationError = "Unable to get GPS location. Try again or use manual address."
                    null
                } else {
                    ResolvedLocation(location.latitude, location.longitude)
                }
            } else {
                val trimmedBarangay = barangay.trim()
                val trimmedCity = city.trim()
                val trimmedProvince = province.trim()
                val fullAddress = "$trimmedBarangay, $trimmedCity, $trimmedProvince, Philippines"
                val geocoder = android.location.Geocoder(context)
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocationName(fullAddress, 1)

                if (addresses.isNullOrEmpty()) {
                    addressError = "Address not found. Please check the details."
                    null
                } else {
                    val address = addresses[0]
                    ResolvedLocation(
                        latitude = address.latitude,
                        longitude = address.longitude,
                        barangay = trimmedBarangay,
                        city = trimmedCity,
                        province = trimmedProvince
                    )
                }
            }
        } catch (e: Exception) {
            if (useGPS) {
                locationError = "Unable to get GPS location. Try again or use manual address."
            } else {
                addressError = "Unable to verify address. Please try again."
            }
            null
        } finally {
            isCapturingLocation = false
        }
    }
    
    fun validateAndSignUp() {
        nameError = null
        emailError = null
        passwordError = null
        confirmPasswordError = null
        addressError = null
        locationError = null

        val trimmedName = name.trim()
        val trimmedEmail = email.trim()
        val trimmedBarangay = barangay.trim()
        val trimmedCity = city.trim()
        val trimmedProvince = province.trim()

        if (trimmedName.isBlank()) {
            nameError = "Name is required"
            return
        }
        if (trimmedEmail.isBlank()) {
            emailError = "Email is required"
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            emailError = "Invalid email format"
            return
        }
        if (password.isBlank()) {
            passwordError = "Password is required"
            return
        }
        if (password.length < 6) {
            passwordError = "Password must be at least 6 characters"
            return
        }
        if (confirmPassword != password) {
            confirmPasswordError = "Passwords do not match"
            return
        }

        if (!useGPS) {
            if (trimmedBarangay.isBlank() || trimmedCity.isBlank() || trimmedProvince.isBlank()) {
                addressError = "Please fill in all address fields"
                return
            }
        } else if (!hasPermission) {
            pendingPermissionSignUp = true
            isLoading = true
            shouldLaunchPermission = true
            return
        }

        isLoading = true
        scope.launch {
            val resolvedLocation = resolveLocation()
            if (resolvedLocation == null) {
                isLoading = false
                return@launch
            }

            try {
                val result = auth.createUserWithEmailAndPassword(trimmedEmail, password).await()
                val userId = result.user?.uid ?: throw Exception("Failed to create user")

                val userData = hashMapOf(
                    "name" to trimmedName,
                    "email" to trimmedEmail,
                    "role" to "user",
                    "barangay" to resolvedLocation.barangay,
                    "city" to resolvedLocation.city,
                    "province" to resolvedLocation.province,
                    "latitude" to resolvedLocation.latitude,
                    "longitude" to resolvedLocation.longitude,
                    "createdAt" to System.currentTimeMillis()
                )

                firestore.collection("users").document(userId).set(userData).await()
                preferences.setUserLocation(resolvedLocation.latitude, resolvedLocation.longitude)
                if (resolvedLocation.barangay != null && resolvedLocation.city != null && resolvedLocation.province != null) {
                    preferences.setUserAddress(
                        resolvedLocation.barangay,
                        resolvedLocation.city,
                        resolvedLocation.province
                    )
                }

                onSignUpSuccess()
            } catch (e: Exception) {
                snackbarHostState.showSnackbar(
                    message = e.message ?: "Sign up failed. Please try again."
                )
                isLoading = false
            }
        }
    }

    // Permission launcher - handles location permission result
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasPermission = granted

        if (pendingPermissionSignUp) {
            pendingPermissionSignUp = false
            if (granted) {
                validateAndSignUp()
            } else {
                isLoading = false
                isCapturingLocation = false
                locationError = "Location permission is required for GPS. Use manual address instead."
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Location permission denied. Switch to manual address to continue."
                    )
                }
            }
        }
    }

    LaunchedEffect(shouldLaunchPermission) {
        if (shouldLaunchPermission) {
            shouldLaunchPermission = false
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }
    
    // Background Gradient consistent with branding
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0F172A), // Dark Slate
            Color(0xFF1E1B4B)  // Deep Indigo
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Account", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .imePadding()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Logo Section (Smaller for Signup)
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.mipmap.ic_launcher),
                        contentDescription = "Logo",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Form Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.05f)
                    ),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { 
                                name = it
                                nameError = null
                            },
                            label = { Text("Full Name", color = Color.White.copy(alpha = 0.7f)) },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color.White.copy(alpha = 0.7f)) },
                            isError = nameError != null,
                            supportingText = nameError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF6366F1),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Color(0xFF6366F1),
                                errorBorderColor = MaterialTheme.colorScheme.error,
                                errorLabelColor = MaterialTheme.colorScheme.error
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OutlinedTextField(
                            value = email,
                            onValueChange = { 
                                email = it
                                emailError = null
                            },
                            label = { Text("Email", color = Color.White.copy(alpha = 0.7f)) },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color.White.copy(alpha = 0.7f)) },
                            isError = emailError != null,
                            supportingText = emailError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF6366F1),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Color(0xFF6366F1),
                                errorBorderColor = MaterialTheme.colorScheme.error,
                                errorLabelColor = MaterialTheme.colorScheme.error
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OutlinedTextField(
                            value = password,
                            onValueChange = { 
                                password = it
                                passwordError = null
                            },
                            label = { Text("Password", color = Color.White.copy(alpha = 0.7f)) },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White.copy(alpha = 0.7f)) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                        tint = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            },
                            isError = passwordError != null,
                            supportingText = passwordError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF6366F1),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Color(0xFF6366F1),
                                errorBorderColor = MaterialTheme.colorScheme.error,
                                errorLabelColor = MaterialTheme.colorScheme.error
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { 
                                confirmPassword = it
                                confirmPasswordError = null
                            },
                            label = { Text("Confirm Password", color = Color.White.copy(alpha = 0.7f)) },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White.copy(alpha = 0.7f)) },
                            isError = confirmPasswordError != null,
                            supportingText = confirmPasswordError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { 
                                    focusManager.clearFocus()
                                    validateAndSignUp()
                                }
                            ),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF6366F1),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Color(0xFF6366F1),
                                errorBorderColor = MaterialTheme.colorScheme.error,
                                errorLabelColor = MaterialTheme.colorScheme.error
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Location Selection Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.Map,
                                contentDescription = null,
                                tint = Color(0xFF6366F1),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                "Home Location",
                                color = Color.White,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // GPS vs Manual Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (useGPS) Color(0xFF6366F1) else Color.Transparent)
                                    .clickable {
                                        useGPS = true
                                        addressError = null
                                        locationError = null
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Use GPS",
                                    color = if (useGPS) Color.White else Color.White.copy(alpha = 0.5f),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (useGPS) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (!useGPS) Color(0xFF6366F1) else Color.Transparent)
                                    .clickable {
                                        useGPS = false
                                        addressError = null
                                        locationError = null
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Manual Address",
                                    color = if (!useGPS) Color.White else Color.White.copy(alpha = 0.5f),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (!useGPS) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                        
                        if (useGPS) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Your precise GPS location will be captured to complete signup.",
                                color = Color.White.copy(alpha = 0.5f),
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (locationError != null) {
                                Text(
                                    locationError!!,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 4.dp).align(Alignment.Start)
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Manual Input Fields
                            OutlinedTextField(
                                value = barangay,
                                onValueChange = { 
                                    barangay = it
                                    addressError = null
                                },
                                label = { Text("Barangay", color = Color.White.copy(alpha = 0.7f)) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF6366F1),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    cursorColor = Color(0xFF6366F1)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            OutlinedTextField(
                                value = city,
                                onValueChange = { 
                                    city = it
                                    addressError = null
                                },
                                label = { Text("City/Municipality", color = Color.White.copy(alpha = 0.7f)) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF6366F1),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    cursorColor = Color(0xFF6366F1)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            OutlinedTextField(
                                value = province,
                                onValueChange = { 
                                    province = it
                                    addressError = null
                                },
                                label = { Text("Province", color = Color.White.copy(alpha = 0.7f)) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF6366F1),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    cursorColor = Color(0xFF6366F1)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            if (addressError != null) {
                                Text(
                                    addressError!!,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 4.dp).align(Alignment.Start)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Button(
                            onClick = { validateAndSignUp() },
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4F46E5), // Indigo 600
                                disabledContainerColor = Color(0xFF4F46E5).copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            if (isLoading) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Text(
                                        if (isCapturingLocation) "Getting Location..." else "Creating Account...",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Text(
                                    "Create Account",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}
