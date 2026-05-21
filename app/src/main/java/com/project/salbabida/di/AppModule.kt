package com.project.salbabida.di

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.project.salbabida.BuildConfig
import com.project.salbabida.data.api.WeatherService
import com.project.salbabida.data.database.SalbaBidaDatabase
import com.project.salbabida.data.database.dao.HomeLocationDao
import com.project.salbabida.data.database.dao.OfflineMarkerDao
import com.project.salbabida.data.database.dao.WeatherCacheDao
import com.project.salbabida.data.preferences.UserPreferences
import com.project.salbabida.data.repository.MapRepository
import com.project.salbabida.data.repository.WeatherRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // ── Network ─────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideWeatherService(client: OkHttpClient): WeatherService {
        return Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/data/2.5/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherService::class.java)
    }

    // ── Database ────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SalbaBidaDatabase {
        return SalbaBidaDatabase.getInstance(context)
    }

    @Provides
    fun provideWeatherCacheDao(db: SalbaBidaDatabase): WeatherCacheDao = db.weatherCacheDao()

    @Provides
    fun provideHomeLocationDao(db: SalbaBidaDatabase): HomeLocationDao = db.homeLocationDao()

    @Provides
    fun provideOfflineMarkerDao(db: SalbaBidaDatabase): OfflineMarkerDao = db.offlineMarkerDao()

    // ── Preferences ─────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideUserPreferences(@ApplicationContext context: Context): UserPreferences {
        return UserPreferences(context)
    }

    // ── Firebase ────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    // ── Repositories ────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideWeatherRepository(
        weatherDao: WeatherCacheDao,
        weatherService: WeatherService
    ): WeatherRepository {
        return WeatherRepository(
            weatherDao = weatherDao,
            weatherService = weatherService,
            apiKey = BuildConfig.OPENWEATHER_API_KEY
        )
    }

    @Provides
    @Singleton
    fun provideMapRepository(
        homeLocationDao: HomeLocationDao,
        offlineMarkerDao: OfflineMarkerDao,
        firestore: FirebaseFirestore
    ): MapRepository {
        return MapRepository(
            homeLocationDao = homeLocationDao,
            offlineMarkerDao = offlineMarkerDao,
            firestore = firestore
        )
    }
}
