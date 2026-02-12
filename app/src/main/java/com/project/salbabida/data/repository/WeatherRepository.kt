package com.project.salbabida.data.repository

import com.project.salbabida.data.api.WeatherService
import com.project.salbabida.data.database.dao.WeatherCacheDao
import com.project.salbabida.data.database.entities.WeatherCache
import kotlinx.coroutines.flow.Flow
import java.io.IOException
import java.util.Locale

/** Typed failure reasons for weather operations. */
sealed class WeatherError(override val message: String) : Exception(message) {
    class Network(cause: Throwable) : WeatherError("Network error: ${cause.localizedMessage}")
    class Server(code: Int) : WeatherError("Server error (HTTP $code)")
    class Unknown(cause: Throwable) : WeatherError(cause.localizedMessage ?: "Unknown error")
}

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

    suspend fun fetchWeatherByCity(city: String): Result<Pair<WeatherCache, String>> = runCatching {
        val response = weatherService.getCurrentWeather(city, apiKey)
        val newCache = response.toWeatherCache(city)
        weatherDao.insertWeather(newCache)
        Pair(newCache, response.name)
    }.recoverCatching { mapThrowable(it) }

    suspend fun fetchWeatherByCoordinates(lat: Double, lon: Double): Result<Pair<WeatherCache, String>> = runCatching {
        val response = weatherService.getWeatherByCoordinates(lat, lon, apiKey)
        val cacheKey = String.format(Locale.US, "%.2f_%.2f", lat, lon)
        val newCache = response.toWeatherCache(cacheKey)
        weatherDao.insertWeather(newCache)
        Pair(newCache, response.name)
    }.recoverCatching { mapThrowable(it) }

    // ── helpers ─────────────────────────────────────────────────────────

    private fun mapThrowable(t: Throwable): Nothing = when (t) {
        is IOException -> throw WeatherError.Network(t)
        is retrofit2.HttpException -> throw WeatherError.Server(t.code())
        else -> throw WeatherError.Unknown(t)
    }

    private fun com.project.salbabida.data.model.WeatherResponse.toWeatherCache(
        cacheKey: String
    ) = WeatherCache(
        city = cacheKey,
        temperature = main?.temp ?: 0.0,
        feelsLike = main?.feelsLike ?: 0.0,
        humidity = main?.humidity ?: 0,
        pressure = main?.pressure ?: 0,
        visibility = visibility,
        windSpeed = wind?.speed ?: 0.0,
        windDeg = wind?.deg ?: 0,
        windGust = wind?.gust,
        cloudiness = clouds?.all ?: 0,
        country = sys?.country ?: "",
        description = weather?.firstOrNull()?.description ?: "",
        icon = weather?.firstOrNull()?.icon ?: ""
    )
}
