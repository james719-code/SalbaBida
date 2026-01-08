package com.project.salbabida.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.project.salbabida.ui.screens.home.HomeScreen
import com.project.salbabida.ui.screens.map.MapScreen
import com.project.salbabida.ui.screens.preparedness.PreparednessScreen
import com.project.salbabida.ui.screens.more.MoreScreen

data class BottomNavItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onLogout: () -> Unit
) {
    val navItems = listOf(
        BottomNavItem("Home", Icons.Filled.Home, Icons.Outlined.Home),
        BottomNavItem("Map", Icons.Filled.Map, Icons.Outlined.Map),
        BottomNavItem("Preparedness", Icons.Filled.MenuBook, Icons.Outlined.MenuBook),
        BottomNavItem("More", Icons.Filled.Settings, Icons.Outlined.Settings)
    )
    
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    
    val colorScheme = MaterialTheme.colorScheme
    val outlineColor = colorScheme.outline
    val backgroundColor = colorScheme.background
    val primaryColor = colorScheme.primary
    val onSurfaceVariantColor = colorScheme.onSurfaceVariant
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = when (selectedIndex) {
                            0 -> "Home"
                            1 -> "Evacuation Map"
                            2 -> "Flood Preparedness"
                            3 -> "More"
                            else -> "SALBA-bida"
                        }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor,
                    titleContentColor = colorScheme.onBackground,
                    actionIconContentColor = colorScheme.onBackground
                ),
                modifier = Modifier.drawBehind {
                    drawLine(
                        color = outlineColor.copy(alpha = 0.2f),
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = backgroundColor,
                tonalElevation = 0.dp,
                modifier = Modifier.drawBehind {
                    drawLine(
                        color = outlineColor.copy(alpha = 0.15f),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            ) {
                navItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedIndex == index,
                        onClick = { selectedIndex = index },
                        label = { 
                            Text(
                                item.title,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selectedIndex == index) FontWeight.Bold else FontWeight.Medium
                            ) 
                        },
                        icon = {
                            Icon(
                                imageVector = if (selectedIndex == index) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.title,
                                tint = if (selectedIndex == index) primaryColor else onSurfaceVariantColor
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = primaryColor,
                            selectedTextColor = primaryColor,
                            unselectedIconColor = onSurfaceVariantColor,
                            unselectedTextColor = onSurfaceVariantColor,
                            indicatorColor = primaryColor.copy(alpha = 0.1f)
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            HomeScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val isSelected = selectedIndex == 0
                        alpha = if (isSelected) 1f else 0f
                        translationX = if (isSelected) 0f else 10000f
                    }
            )
            MapScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val isSelected = selectedIndex == 1
                        alpha = if (isSelected) 1f else 0f
                        translationX = if (isSelected) 0f else 10000f
                    }
            )
            PreparednessScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val isSelected = selectedIndex == 2
                        alpha = if (isSelected) 1f else 0f
                        translationX = if (isSelected) 0f else 10000f
                    }
            )
            MoreScreen(
                onLogout = onLogout,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val isSelected = selectedIndex == 3
                        alpha = if (isSelected) 1f else 0f
                        translationX = if (isSelected) 0f else 10000f
                    }
            )
        }
    }
}
