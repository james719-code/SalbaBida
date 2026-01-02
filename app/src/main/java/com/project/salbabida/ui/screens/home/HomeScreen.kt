package com.project.salbabida.ui.screens.home

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
    var selectedCity by remember { mutableStateOf("") }
    
    val scope = rememberCoroutineScope()
    val app = SalbaBidaApplication.getInstance()
    val weatherDao = app.database.weatherCacheDao()
    val preferences = app.userPreferences
    
    suspend fun fetchWeather(forceRefresh: Boolean = false) {
        try {
            val city = preferences.selectedCity.first() ?: "Sorsogon City"
            selectedCity = city
            
            // Check cache first
            val cached = weatherDao.getWeatherForCity(city)
            if (cached != null && !cached.isExpired() && !forceRefresh) {
                weatherData = cached
                isLoading = false
                return
            }
            
            // Fetch from API
            val response = RetrofitClient.weatherService.getCurrentWeather(city, API_KEY)
            
            val newCache = WeatherCache(
                city = city,
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
            // Try to show cached data even if it's expired
            val cached = weatherDao.getWeatherForCity(selectedCity)
            if (cached != null) {
                weatherData = cached
            }
        } finally {
            isLoading = false
            isRefreshing = false
        }
    }
    
    LaunchedEffect(Unit) {
        fetchWeather()
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
                        WeatherHeaderCard(weather, selectedCity)
                    }
                    
                    item {
                        Text(
                            text = "Weather Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            WeatherDetailCard(
                                icon = Icons.Default.Thermostat,
                                title = "Feels Like",
                                value = String.format("%.1f°C", weather.feelsLike),
                                modifier = Modifier.weight(1f)
                            )
                            WeatherDetailCard(
                                icon = Icons.Default.WaterDrop,
                                title = "Humidity",
                                value = "${weather.humidity}%",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            WeatherDetailCard(
                                icon = Icons.Default.Air,
                                title = "Wind Speed",
                                value = String.format("%.1f m/s", weather.windSpeed),
                                modifier = Modifier.weight(1f)
                            )
                            WeatherDetailCard(
                                icon = Icons.Default.Cloud,
                                title = "Cloudiness",
                                value = "${weather.cloudiness}%",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            WeatherDetailCard(
                                icon = Icons.Default.Visibility,
                                title = "Visibility",
                                value = "${weather.visibility / 1000} km",
                                modifier = Modifier.weight(1f)
                            )
                            WeatherDetailCard(
                                icon = Icons.Default.Thermostat,
                                title = "Pressure",
                                value = "${weather.pressure} hPa",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    
                    item {
                        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                        Text(
                            text = "Last updated: ${dateFormat.format(Date(weather.lastUpdated))}",
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
private fun WeatherHeaderCard(weather: WeatherCache, city: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = city,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = weather.country,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = String.format("%.1f°C", weather.temperature),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = weather.description.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
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
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
