package com.project.salbabida.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.salbabida.SalbaBidaApplication
import com.project.salbabida.data.api.RetrofitClient
import com.project.salbabida.data.database.entities.WeatherCache
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val API_KEY = "276d3e172aefb4795f6f7d94069e9c2b"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    var weatherData by remember { mutableStateOf<WeatherCache?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var locationName by remember { mutableStateOf("") }
    
    val scope = rememberCoroutineScope()
    val app = SalbaBidaApplication.getInstance()
    val weatherDao = app.database.weatherCacheDao()
    val preferences = app.userPreferences
    
    val weatherLat by preferences.weatherLatitude.collectAsState(initial = null)
    val weatherLon by preferences.weatherLongitude.collectAsState(initial = null)
    val savedLocationName by preferences.weatherLocationName.collectAsState(initial = null)
    
    suspend fun fetchWeather(forceRefresh: Boolean = false) {
            val lat = weatherLat
            val lon = weatherLon
            val city = preferences.selectedCity.first() ?: "Sorsogon City"
            
            val cacheKey = if (lat != null && lon != null) {
                "${lat.toInt()}_${lon.toInt()}"
            } else {
                city
            }

            try {
            
            locationName = savedLocationName ?: city
            
            val cached = weatherDao.getWeatherForCity(cacheKey)
            
            // 1. Load cache immediately if available (even if expired or refreshing)
            if (cached != null) {
                weatherData = cached
            }

            // 2. If cache is valid and we don't need to force refresh, stop here.
            if (cached != null && !cached.isExpired() && !forceRefresh) {
                isLoading = false
                return
            }
            
            val response = if (lat != null && lon != null) {
                RetrofitClient.weatherService.getWeatherByCoordinates(lat, lon, API_KEY)
            } else {
                RetrofitClient.weatherService.getCurrentWeather(city, API_KEY)
            }
            
            locationName = response.name
            
            val newCache = WeatherCache(
                city = cacheKey,
                temperature = response.main?.temp ?: 0.0,
                feelsLike = response.main?.feelsLike ?: 0.0,
                humidity = response.main?.humidity ?: 0,
                pressure = response.main?.pressure ?: 0,
                visibility = response.visibility,
                windSpeed = response.wind?.speed ?: 0.0,
                windDeg = response.wind?.deg ?: 0,
                windGust = response.wind?.gust,
                cloudiness = response.clouds?.all ?: 0,
                country = response.sys?.country ?: "",
                description = response.weather?.firstOrNull()?.description ?: "",
                icon = response.weather?.firstOrNull()?.icon ?: ""
            )
            
            weatherDao.insertWeather(newCache)
            weatherData = newCache
            error = null
        } catch (e: Exception) {
            error = e.message ?: "Failed to fetch weather"
            // If we haven't loaded data yet (no cache at start), try one last time or keep error
            if (weatherData == null) {
                val cached = weatherDao.getWeatherForCity(cacheKey)
                if (cached != null) {
                    weatherData = cached
                }
            }
        } finally {
            isLoading = false
            isRefreshing = false
        }
    }
    
    LaunchedEffect(weatherLat, weatherLon) {
        fetchWeather(forceRefresh = false)
    }
    
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            scope.launch { fetchWeather(forceRefresh = true) }
        },
        modifier = modifier
    ) {
        if (isLoading && weatherData == null) {
            WeatherLoadingSkeleton()
        } else if (weatherData == null && error != null) {
            ErrorState(error = error ?: "Unknown error") {
                isLoading = true
                scope.launch { fetchWeather(forceRefresh = true) }
            }
        } else {
            weatherData?.let { weather ->
                WeatherContent(weather, locationName)
            }
        }
    }
}

@Composable
fun ErrorState(error: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Cloud,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Unable to load weather",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        IconButton(
            onClick = onRetry,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                .padding(4.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
fun WeatherContent(weather: WeatherCache, locationName: String) {
    val detailItems = remember(weather) {
        listOf(
            WeatherDetailItem(Icons.Default.Thermostat, "Feels Like", String.format("%.1f", weather.feelsLike) + "°C"),
            WeatherDetailItem(Icons.Default.WaterDrop, "Humidity", "${weather.humidity}%"),
            WeatherDetailItem(Icons.Default.Air, "Wind", String.format("%.1f", weather.windSpeed) + " m/s"),
            WeatherDetailItem(Icons.Default.Cloud, "Clouds", "${weather.cloudiness}%"),
            WeatherDetailItem(Icons.Default.Visibility, "Visibility", "${weather.visibility / 1000} km"),
            WeatherDetailItem(Icons.Default.Thermostat, "Pressure", "${weather.pressure} hPa")
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            WeatherHeaderCard(weather, locationName)
        }
        
        item {
            FloodAlertCard(weather)
        }
        
        item {
            Text(
                text = "Current Conditions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
        }

        // Grid Logic
        items(detailItems.chunked(2)) { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                rowItems.forEach { item ->
                    WeatherDetailCard(
                        item = item,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1.4f)
                    )
                }
                // Handle odd number of items if necessary
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
        
        item {
            val dateFormat = SimpleDateFormat("MMMM dd · HH:mm", Locale.getDefault())
            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Updated ${dateFormat.format(Date(weather.lastUpdated))}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

data class WeatherDetailItem(val icon: ImageVector, val label: String, val value: String)

@Composable
private fun WeatherHeaderCard(weather: WeatherCache, location: String) {
    // Dynamic Gradient based on weather description
    val gradientColors = when {
        weather.description.contains("clear") -> listOf(Color(0xFFFDB813), Color(0xFFF57F17)) // Sunny Gold
        weather.description.contains("rain") || weather.description.contains("drizzle") -> listOf(Color(0xFF37474F), Color(0xFF455A64), Color(0xFF546E7A)) // Stormy Blue-Grey
        weather.description.contains("cloud") -> listOf(Color(0xFF78909C), Color(0xFF90A4AE)) // Cloudy Blue-Grey
        weather.description.contains("storm") -> listOf(Color(0xFF263238), Color(0xFF37474F)) // Dark Storm
        else -> listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(Brush.linearGradient(gradientColors))
                .fillMaxWidth()
        ) {
            // subtle artistic circle overlay
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 50.dp, y = (-50).dp)
                    .background(Color.White.copy(alpha = 0.1f), CircleShape)
            )
            
            Column(
                modifier = Modifier
                    .padding(28.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.15f), RoundedCornerShape(50))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = location.uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = String.format("%.0f", weather.temperature) + "°",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 96.sp,
                        lineHeight = 96.sp
                    ),
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                
                Text(
                    text = weather.description.split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } },
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Normal
                )
                
                Text(
                    text = "H:${String.format("%.0f", weather.temperature + 2)}°  L:${String.format("%.0f", weather.temperature - 2)}°", // Estimation for UI visual
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun FloodAlertCard(weather: WeatherCache) {
    val description = weather.description.lowercase()
    val isRainy = description.contains("rain") || 
                  description.contains("storm") || 
                  description.contains("thunderstorm") ||
                  description.contains("drizzle")
    
    val isHighRisk = weather.humidity > 85 && weather.cloudiness > 80
    // Logic for displaying the alert
    val showAlert = isRainy || isHighRisk
    
    val (title, message, containerColor, contentColor) = when {
        description.contains("thunderstorm") || description.contains("storm") -> 
            Trace(
                "Severe Weather Alert",
                "Flash floods possible in low lying areas. Seek shelter.",
                MaterialTheme.colorScheme.errorContainer,
                MaterialTheme.colorScheme.onErrorContainer
            )
        description.contains("heavy rain") ->
            Trace(
                "Heavy Rain Warning",
                "High risk of flooding. Monitor local advisories.",
                MaterialTheme.colorScheme.errorContainer,
                MaterialTheme.colorScheme.onErrorContainer
            )
        isRainy ->
             Trace(
                "Rainy Conditions",
                "Roads may be slippery. Low flood risk but stay alert.",
                MaterialTheme.colorScheme.secondaryContainer,
                MaterialTheme.colorScheme.onSecondaryContainer
            )
        isHighRisk ->
            Trace(
                "High Humidity",
                "Conditions are favorable for rain. Keep an umbrella handy.",
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.onSurfaceVariant
            )
        else -> Trace("", "", Color.Transparent, Color.Transparent)
    }
    
    AnimatedVisibility(
        visible = showAlert,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = containerColor)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = if (contentColor == MaterialTheme.colorScheme.onErrorContainer) Icons.Default.Warning else Icons.Default.WaterDrop,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}

// Data class mainly for tuple holding in the when block above
data class Trace(val title: String, val msg: String, val bg: Color, val fg: Color)

@Composable
private fun WeatherDetailCard(
    item: WeatherDetailItem,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Column {
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = item.value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun WeatherLoadingSkeleton() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header Skeleton
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                shape = RoundedCornerShape(32.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(32.dp))
                        .shimmerEffect()
                )
            }
        }
        
        // Alert Skeleton
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .shimmerEffect()
            )
        }
        
        // Title Skeleton
        item {
            Box(
                modifier = Modifier
                    .width(150.dp)
                    .height(24.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .shimmerEffect()
            )
        }

        // Grid Skeleton (6 items)
        items(6) { index ->
             if (index % 2 == 0) {
                 Row(
                     modifier = Modifier.fillMaxWidth(),
                     horizontalArrangement = Arrangement.spacedBy(16.dp)
                 ) {
                     Box(
                         modifier = Modifier
                             .weight(1f)
                             .aspectRatio(1.4f)
                             .clip(RoundedCornerShape(24.dp))
                             .shimmerEffect()
                     )
                     Box(
                         modifier = Modifier
                             .weight(1f)
                             .aspectRatio(1.4f)
                             .clip(RoundedCornerShape(24.dp))
                             .shimmerEffect()
                     )
                 }
                 Spacer(modifier = Modifier.height(20.dp))
             }
        }
    }
}

fun Modifier.shimmerEffect(): Modifier = composed {
    val shimmerColors = listOf(
        Color.LightGray.copy(alpha = 0.3f),
        Color.LightGray.copy(alpha = 0.5f),
        Color.LightGray.copy(alpha = 0.3f),
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_loading"
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )

    background(brush)
}
