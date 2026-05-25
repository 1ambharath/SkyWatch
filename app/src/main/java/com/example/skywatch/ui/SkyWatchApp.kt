package com.example.skywatch.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.skywatch.ui.navigation.SkyWatchNavGraph
import com.example.skywatch.ui.navigation.NavRequest
import com.example.skywatch.ui.navigation.SkyWatchRoutes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkyWatchApp(
    navRequest: NavRequest?,
    onNavHandled: () -> Unit,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route

    val title = when {
        route == SkyWatchRoutes.Home -> "SkyWatch"
        route == SkyWatchRoutes.Settings -> "Settings"
        route == SkyWatchRoutes.FlightList -> "Flights"
        route?.startsWith(SkyWatchRoutes.FlightDetailsPrefix) == true -> "Flight details"
        else -> "SkyWatch"
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Scaffold(
            topBar = {
                TopAppBar(title = { Text(title) })
            }
        ) { innerPadding ->
            LaunchedEffect(navRequest) {
                when (val req = navRequest) {
                    null -> Unit
                    NavRequest.Flights -> {
                        navController.navigate(SkyWatchRoutes.FlightList) { launchSingleTop = true }
                        onNavHandled()
                    }
                    is NavRequest.FlightDetails -> {
                        navController.navigate(SkyWatchRoutes.flightDetails(req.flightId)) { launchSingleTop = true }
                        onNavHandled()
                    }
                }
            }
            SkyWatchNavGraph(
                navController = navController,
                contentPadding = innerPadding
            )
        }
    }
}

