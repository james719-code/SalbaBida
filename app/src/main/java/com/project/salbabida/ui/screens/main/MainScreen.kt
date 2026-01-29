package com.project.salbabida.ui.screens.main

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.project.salbabida.navigation.BottomNavRoute
import com.project.salbabida.ui.screens.home.HomeScreen
import com.project.salbabida.ui.screens.map.MapScreen
import com.project.salbabida.ui.screens.more.MoreScreen
import com.project.salbabida.ui.screens.preparedness.PreparednessScreen
import com.project.salbabida.ui.screens.safety.SafetyScreen

data class BottomNavItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val route: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onLogout: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val navItems = listOf(
        BottomNavItem("Home", Icons.Filled.Home, Icons.Outlined.Home, BottomNavRoute.Home.route),
        BottomNavItem("Map", Icons.Filled.Map, Icons.Outlined.Map, BottomNavRoute.Map.route),
        BottomNavItem("Safety", Icons.Filled.HealthAndSafety, Icons.Outlined.HealthAndSafety, BottomNavRoute.Safety.route),
        BottomNavItem("Preparedness", Icons.Filled.MenuBook, Icons.Outlined.MenuBook, BottomNavRoute.Preparedness.route),
        BottomNavItem("More", Icons.Filled.Settings, Icons.Outlined.Settings, BottomNavRoute.More.route)
    )

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
                        text = when (currentDestination?.route) {
                            BottomNavRoute.Home.route -> "Home"
                            BottomNavRoute.Map.route -> "Evacuation Map"
                            BottomNavRoute.Safety.route -> "Safety Tools"
                            BottomNavRoute.Preparedness.route -> "Flood Preparedness"
                            BottomNavRoute.More.route -> "More"
                            else -> "SALBA-bida"
                        },
                        fontWeight = FontWeight.Bold
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
                navItems.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        label = {
                            Text(
                                item.title,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.title,
                                tint = if (selected) primaryColor else onSurfaceVariantColor
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
        NavHost(
            navController = navController,
            startDestination = BottomNavRoute.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            composable(BottomNavRoute.Home.route) {
                HomeScreen(modifier = Modifier.fillMaxSize())
            }
            composable(BottomNavRoute.Map.route) {
                MapScreen(modifier = Modifier.fillMaxSize())
            }
            composable(BottomNavRoute.Safety.route) {
                SafetyScreen(modifier = Modifier.fillMaxSize())
            }
            composable(BottomNavRoute.Preparedness.route) {
                PreparednessScreen(modifier = Modifier.fillMaxSize())
            }
            composable(BottomNavRoute.More.route) {
                MoreScreen(onLogout = onLogout, modifier = Modifier.fillMaxSize())
            }
        }
    }
}
