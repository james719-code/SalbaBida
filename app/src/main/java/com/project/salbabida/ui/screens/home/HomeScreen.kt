package com.project.salbabida.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
        try {
            val lat = weatherLat
            val lon = weatherLon
            val city = preferences.selectedCity.first() ?: "Sorsogon City"
            
            val cacheKey = if (lat != null && lon != null) {
                "${lat.toInt()}_${lon.toInt()}"
            } else {
                city
            }
            
            locationName = savedLocationName ?: city
            
            val cached = weatherDao.getWeatherForCity(cacheKey)
            if (cached != null && !cached.isExpired() && !forceRefresh) {
                weatherData = cached
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
            val city = preferences.selectedCity.first() ?: "Sorsogon City"
            val cached = weatherDao.getWeatherForCity(city)
            if (cached != null) {
                weatherData = cached
            }
        } finally {
            isLoading = false
            isRefreshing = false
        }
    }
    
    LaunchedEffect(weatherLat, weatherLon) {
        // Initial load or location change should NOT force refresh if cache is valid
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
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (weatherData == null && error != null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Unable to load weather data",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                IconButton(onClick = {
                    isLoading = true
                    scope.launch { fetchWeather(forceRefresh = true) }
                }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Retry")
                }
            }
        } else {
            weatherData?.let { weather ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        WeatherHeaderCard(weather, locationName)
                    }
                    
                    item {
                        FloodAlertCard(weather)
                    }
                    
                    item {
                        Text(
                            text = "Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Weather Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            WeatherDetailCard(
                                icon = Icons.Default.Thermostat,
                                title = "Feels Like",
                                value = String.format("%.1f", weather.feelsLike) + "\u00B0C"
                            )
                            WeatherDetailCard(
                                icon = Icons.Default.WaterDrop,
                                title = "Humidity",
                                value = "${weather.humidity}%"
                            )
                            WeatherDetailCard(
                                icon = Icons.Default.Air,
                                title = "Wind",
                                value = String.format("%.1f", weather.windSpeed) + " m/s"
                            )
                            WeatherDetailCard(
                                icon = Icons.Default.Cloud,
                                title = "Clouds",
                                value = "${weather.cloudiness}%"
                            )
                            WeatherDetailCard(
                                icon = Icons.Default.Visibility,
                                title = "Visibility",
                                value = "${weather.visibility / 1000} km"
                            )
                            WeatherDetailCard(
                                icon = Icons.Default.Thermostat,
                                title = "Pressure",
                                value = "${weather.pressure} hPa"
                            )
                        }
                    }
                    
                    item {
                        val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                        Text(
                            text = "Updated: ${dateFormat.format(Date(weather.lastUpdated))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherHeaderCard(weather: WeatherCache, location: String) {
    val gradientColors = when {
        weather.description.contains("clear") -> listOf(Color(0xFFFDC830), Color(0xFFF37335))
        weather.description.contains("rain") || weather.description.contains("drizzle") -> listOf(Color(0xFF4B79A1), Color(0xFF283E51))
        weather.description.contains("cloud") -> listOf(Color(0xFFbdc3c7), Color(0xFF2c3e50))
        weather.description.contains("storm") -> listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
        else -> listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .background(Brush.linearGradient(gradientColors))
                .padding(32.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = location.uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.9f),
                        letterSpacing = 1.5.sp
                    )
                }
                
                Text(
                    text = weather.country,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = String.format("%.0f", weather.temperature) + "\u00B0",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 80.sp,
                        lineHeight = 80.sp
                    ),
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Surface(
                    color = Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = weather.description.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
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
                  description.contains("drizzle") ||
                  description.contains("shower")
    
    val isHighRisk = weather.humidity > 85 && weather.cloudiness > 80
    val showAlert = isRainy || isHighRisk
    
    val alertMessage = when {
        description.contains("thunderstorm") || description.contains("storm") -> 
            "Severe weather alert: Possible flooding in low-lying areas. Seek shelter immediately."
        description.contains("heavy rain") || (isRainy && weather.humidity > 90) ->
            "Heavy rain detected: High risk of flash floods. Avoid flood-prone areas."
        isRainy ->
            "Rain expected: Be aware of possible flooding in flood-prone areas."
        isHighRisk ->
            "High humidity and cloud cover: Rain likely. Stay alert for potential flooding."
        else -> ""
    }
    
    AnimatedVisibility(
        visible = showAlert,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
                Column {
                    Text(
                        text = "Flood Warning",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = alertMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun WeatherDetailCard(
    icon: ImageVector,
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
