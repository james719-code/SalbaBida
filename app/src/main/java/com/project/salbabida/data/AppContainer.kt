package com.project.salbabida.data

import android.content.Context
import com.project.salbabida.BuildConfig
import com.project.salbabida.data.api.RetrofitClient
import com.project.salbabida.data.database.SalbaBidaDatabase
import com.project.salbabida.data.repository.WeatherRepository

interface AppContainer {
    val weatherRepository: WeatherRepository
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    private val database: SalbaBidaDatabase by lazy {
        SalbaBidaDatabase.getInstance(context)
    }

    override val weatherRepository: WeatherRepository by lazy {
        WeatherRepository(
            weatherDao = database.weatherCacheDao(),
            weatherService = RetrofitClient.weatherService,
            apiKey = BuildConfig.OPENWEATHER_API_KEY
        )
    }
}
