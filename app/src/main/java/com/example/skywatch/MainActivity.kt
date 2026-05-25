package com.example.skywatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.skywatch.ui.SkyWatchApp
import com.example.skywatch.ui.navigation.NavIntents
import com.example.skywatch.ui.navigation.NavRequest
import com.example.skywatch.ui.theme.SkyWatchTheme

class MainActivity : ComponentActivity() {
    private var navRequestState by mutableStateOf<NavRequest?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        navRequestState = intent.toNavRequestOrNull()
        setContent {
            SkyWatchTheme {
                SkyWatchApp(
                    navRequest = navRequestState,
                    onNavHandled = { navRequestState = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        navRequestState = intent.toNavRequestOrNull()
    }
}

private fun android.content.Intent.toNavRequestOrNull(): NavRequest? {
    return when (getStringExtra(NavIntents.ExtraNavTarget)) {
        NavIntents.TargetFlights -> NavRequest.Flights
        NavIntents.TargetFlightDetails -> {
            val flightId = getStringExtra(NavIntents.ExtraFlightId).orEmpty()
            if (flightId.isBlank()) null else NavRequest.FlightDetails(flightId)
        }
        else -> null
    }
}