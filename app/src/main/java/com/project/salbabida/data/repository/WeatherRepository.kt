package com.project.salbabida.data.repository

import com.project.salbabida.data.api.WeatherService
import com.project.salbabida.data.database.dao.WeatherCacheDao
import com.project.salbabida.data.database.entities.WeatherCache
import kotlinx.coroutines.flow.Flow
import java.util.Locale

class WeatherRepository(
    private val weatherDao: WeatherCacheDao,
    private val weatherService: WeatherService,
    private val apiKey: String
) {

    fun observeWeather(cacheKey: String): Flow<WeatherCache?> {
        return weatherDao.observeWeatherForCity(cacheKey)
    }

    suspend fun getCachedWeather(cacheKey: String): WeatherCache? {
        return weatherDao.getWeatherForCity(cacheKey)
    }

    suspend fun fetchWeatherByCity(city: String): Pair<WeatherCache, String> {
        val response = weatherService.getCurrentWeather(city, apiKey)
        
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
        return Pair(newCache, response.name)
    }

    suspend fun fetchWeatherByCoordinates(lat: Double, lon: Double): Pair<WeatherCache, String> {
        val response = weatherService.getWeatherByCoordinates(lat, lon, apiKey)
        val cacheKey = String.format(Locale.US, "%.4f_%.4f", lat, lon)

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
        return Pair(newCache, response.name)
    }
}
