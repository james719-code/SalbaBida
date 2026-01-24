package com.project.salbabida.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "salbabida_preferences")

class UserPreferences(private val context: Context) {
    
    companion object {
        private val KEY_SELECTED_CITY = stringPreferencesKey("selected_city")
        private val KEY_TERMS_ACCEPTED = booleanPreferencesKey("terms_accepted")
        private val KEY_DARK_THEME = booleanPreferencesKey("dark_theme")
        private val KEY_DYNAMIC_COLORS = booleanPreferencesKey("dynamic_colors")
        private val KEY_ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        private val KEY_USER_LATITUDE = doublePreferencesKey("user_latitude")
        private val KEY_USER_LONGITUDE = doublePreferencesKey("user_longitude")
        private val KEY_USER_BARANGAY = stringPreferencesKey("user_barangay")
        private val KEY_USER_CITY = stringPreferencesKey("user_city")
        private val KEY_USER_PROVINCE = stringPreferencesKey("user_province")
        private val KEY_WEATHER_LATITUDE = doublePreferencesKey("weather_latitude")
        private val KEY_WEATHER_LONGITUDE = doublePreferencesKey("weather_longitude")
        private val KEY_WEATHER_LOCATION_NAME = stringPreferencesKey("weather_location_name")
        private val KEY_USER_ROLE = stringPreferencesKey("user_role")
    }
    
    val selectedCity: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_SELECTED_CITY]
    }
    
    val termsAccepted: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_TERMS_ACCEPTED] ?: false
    }
    
    val isDarkTheme: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_DARK_THEME] ?: false
    }
    
    val useDynamicColors: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_DYNAMIC_COLORS] ?: true
    }
    
    val isOnboardingComplete: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_ONBOARDING_COMPLETE] ?: false
    }
    
    val userLatitude: Flow<Double?> = context.dataStore.data.map { preferences ->
        preferences[KEY_USER_LATITUDE]
    }
    
    val userLongitude: Flow<Double?> = context.dataStore.data.map { preferences ->
        preferences[KEY_USER_LONGITUDE]
    }

    val userBarangay: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_USER_BARANGAY]
    }

    val userCity: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_USER_CITY]
    }

    val userProvince: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_USER_PROVINCE]
    }
    
    val weatherLatitude: Flow<Double?> = context.dataStore.data.map { preferences ->
        preferences[KEY_WEATHER_LATITUDE]
    }
    
    val weatherLongitude: Flow<Double?> = context.dataStore.data.map { preferences ->
        preferences[KEY_WEATHER_LONGITUDE]
    }
    
    val weatherLocationName: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_WEATHER_LOCATION_NAME]
    }

    val userRole: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_USER_ROLE]
    }
    
    suspend fun setSelectedCity(city: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SELECTED_CITY] = city
        }
    }
    
    suspend fun setTermsAccepted(accepted: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_TERMS_ACCEPTED] = accepted
        }
    }
    
    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DARK_THEME] = enabled
        }
    }
    
    suspend fun setDynamicColors(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DYNAMIC_COLORS] = enabled
        }
    }
    
    suspend fun setOnboardingComplete(complete: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_ONBOARDING_COMPLETE] = complete
        }
    }
    
    suspend fun setUserLocation(latitude: Double, longitude: Double) {
        context.dataStore.edit { preferences ->
            preferences[KEY_USER_LATITUDE] = latitude
            preferences[KEY_USER_LONGITUDE] = longitude
        }
    }

    suspend fun setUserAddress(barangay: String, city: String, province: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_USER_BARANGAY] = barangay
            preferences[KEY_USER_CITY] = city
            preferences[KEY_USER_PROVINCE] = province
        }
    }
    
    suspend fun setWeatherLocation(latitude: Double, longitude: Double, locationName: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_WEATHER_LATITUDE] = latitude
            preferences[KEY_WEATHER_LONGITUDE] = longitude
            preferences[KEY_WEATHER_LOCATION_NAME] = locationName
        }
    }

    suspend fun setUserRole(role: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_USER_ROLE] = role
        }
    }
}

