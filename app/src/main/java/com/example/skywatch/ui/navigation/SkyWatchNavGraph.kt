package com.example.skywatch.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.skywatch.ui.screens.FlightDetailsScreen
import com.example.skywatch.ui.screens.FlightListScreen
import com.example.skywatch.ui.screens.HomeScreen
import com.example.skywatch.ui.screens.SettingsScreen

@Composable
fun SkyWatchNavGraph(
    navController: NavHostController,
    contentPadding: PaddingValues,
) {
    NavHost(
        navController = navController,
        startDestination = SkyWatchRoutes.Home,
        modifier = Modifier.padding(contentPadding),
    ) {
        composable(SkyWatchRoutes.Home) {
            HomeScreen(
                onOpenSettings = { navController.navigate(SkyWatchRoutes.Settings) },
                onOpenFlights = { navController.navigate(SkyWatchRoutes.FlightList) },
            )
        }

        composable(SkyWatchRoutes.Settings) {
            SettingsScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(SkyWatchRoutes.FlightList) {
            FlightListScreen(
                onNavigateBack = { navController.popBackStack() },
                onOpenFlight = { flightId -> navController.navigate(SkyWatchRoutes.flightDetails(flightId)) },
            )
        }

        composable(
            route = SkyWatchRoutes.FlightDetails,
            arguments = listOf(
                navArgument(SkyWatchRoutes.FlightDetailsArgFlightId) { type = NavType.StringType }
            ),
        ) { entry ->
            val flightId =
                entry.arguments?.getString(SkyWatchRoutes.FlightDetailsArgFlightId).orEmpty()
            FlightDetailsScreen(
                flightId = flightId,
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}

