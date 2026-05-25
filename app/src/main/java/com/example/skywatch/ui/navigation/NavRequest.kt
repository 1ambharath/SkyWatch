package com.example.skywatch.ui.navigation

sealed interface NavRequest {
    data object Flights : NavRequest
    data class FlightDetails(val flightId: String) : NavRequest
}

