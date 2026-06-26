package me.mavenried.Ryde.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import me.mavenried.Ryde.ui.screens.HeatmapScreen
import me.mavenried.Ryde.ui.screens.HistoryScreen
import me.mavenried.Ryde.ui.screens.HomeScreen
import me.mavenried.Ryde.ui.screens.PersonalRecordsScreen
import me.mavenried.Ryde.ui.screens.RouteDetailScreen
import me.mavenried.Ryde.ui.screens.SettingsScreen
import me.mavenried.Ryde.ui.screens.StatsScreen

private object Routes {
    const val HOME = "home"
    const val HISTORY = "history"
    const val ROUTE_DETAIL = "route/{routeId}"
    const val SETTINGS = "settings"
    const val PERSONAL_RECORDS = "personal_records"
    const val STATS = "stats"
    const val HEATMAP = "heatmap"
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
                onRouteClick = { id -> nav.navigate(Routes.routeDetail(id)) },
                onNavigateToPersonalRecords = { nav.navigate(Routes.PERSONAL_RECORDS) },
                onNavigateToStats = { nav.navigate(Routes.STATS) },
                onNavigateToHeatmap = { nav.navigate(Routes.HEATMAP) }
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
        composable(Routes.PERSONAL_RECORDS) {
            PersonalRecordsScreen(
                onNavigateBack = { nav.popBackStack() },
                onRouteClick = { id -> nav.navigate(Routes.routeDetail(id)) }
            )
        }
        composable(Routes.STATS) {
            StatsScreen(onNavigateBack = { nav.popBackStack() })
        }
        composable(Routes.HEATMAP) {
            HeatmapScreen(onNavigateBack = { nav.popBackStack() })
        }
    }
}
