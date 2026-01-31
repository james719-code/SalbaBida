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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
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
            Box(
                modifier = Modifier
                    .padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
                    .fillMaxWidth()
            ) {
                // Floating Capsule Surface
                androidx.compose.material3.Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 3.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(vertical = 12.dp, horizontal = 12.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        navItems.forEach { item ->
                            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                            val iconColor by animateColorAsState(
                                if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                label = "iconColor"
                            )
                            val containerColor by animateColorAsState(
                                if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                label = "containerColor"
                            )
                            val scale by animateFloatAsState(
                                if (selected) 1.1f else 1.0f,
                                label = "scale"
                            )

                            // Custom Navigation Item
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null, // Handled by containerColor or default ripple if needed, but here we want smooth color transition
                                        onClick = {
                                            navController.navigate(item.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )
                                    .background(containerColor)
                                    .padding(vertical = 10.dp, horizontal = 16.dp), // Comfortable touch target
                                contentAlignment = androidx.compose.ui.Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.title,
                                        tint = iconColor,
                                        modifier = Modifier
                                            .scale(scale)
                                            .size(24.dp)
                                    )
                                    
                                    // Show label ONLY when selected to save space and look premium
                                    AnimatedVisibility(
                                        visible = selected,
                                        enter = fadeIn() + expandHorizontally(),
                                        exit = fadeOut() + shrinkHorizontally()
                                    ) {
                                        Text(
                                            text = item.title,
                                            color = iconColor,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(start = 8.dp),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
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
