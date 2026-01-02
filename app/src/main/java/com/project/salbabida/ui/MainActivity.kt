package com.project.salbabida.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.project.salbabida.navigation.AppNavigation
import com.project.salbabida.SalbaBidaApplication
import com.project.salbabida.ui.theme.SalbaBidaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val userPreferences = SalbaBidaApplication.getInstance().userPreferences
        
        setContent {
            val isDarkTheme by userPreferences.isDarkTheme.collectAsState(initial = false)
            val useDynamicColors by userPreferences.useDynamicColors.collectAsState(initial = true)
            
            SalbaBidaTheme(
                darkTheme = isDarkTheme,
                dynamicColor = useDynamicColors
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    AppNavigation(navController = navController)
                }
            }
        }
    }
}
