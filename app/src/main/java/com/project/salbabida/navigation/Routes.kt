package com.project.salbabida.navigation

sealed class Route(val route: String) {
    data object Splash : Route("splash")
    data object Login : Route("login")
    data object SignUp : Route("signup")
    data object CitySelection : Route("city_selection")
    data object Main : Route("main")
    data object Settings : Route("settings")
    data object About : Route("about")
}

sealed class BottomNavRoute(val route: String) {
    data object Home : BottomNavRoute("home")
    data object Map : BottomNavRoute("map")
    data object Preparedness : BottomNavRoute("preparedness")
}
