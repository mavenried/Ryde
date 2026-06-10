package me.mavenried.Ryde.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import me.mavenried.Ryde.ui.screens.HistoryScreen
import me.mavenried.Ryde.ui.screens.HomeScreen
import me.mavenried.Ryde.ui.screens.RouteDetailScreen
import me.mavenried.Ryde.ui.screens.SettingsScreen

private object Routes {
    const val HOME = "home"
    const val HISTORY = "history"
    const val ROUTE_DETAIL = "route/{routeId}"
    const val SETTINGS = "settings"
    fun routeDetail(id: String) = "route/$id"
}

@Composable
fun AppNavigation() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToHistory = { nav.navigate(Routes.HISTORY) },
                onNavigateToSettings = { nav.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.HISTORY) {
            HistoryScreen(
                onNavigateBack = { nav.popBackStack() },
                onRouteClick = { id -> nav.navigate(Routes.routeDetail(id)) }
            )
        }
        composable(
            route = Routes.ROUTE_DETAIL,
            arguments = listOf(navArgument("routeId") { type = NavType.StringType })
        ) { back ->
            RouteDetailScreen(
                routeId = back.arguments?.getString("routeId") ?: "",
                onNavigateBack = { nav.popBackStack() }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onNavigateBack = { nav.popBackStack() })
        }
    }
}
