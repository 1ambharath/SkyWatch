package com.example.skywatch.ui.navigation

object SkyWatchRoutes {
    const val Home = "home"
    const val Settings = "settings"
    const val FlightList = "flights"

    const val FlightDetailsArgFlightId = "flightId"
    const val FlightDetailsPrefix = "flight/"
    const val FlightDetails = "flight/{$FlightDetailsArgFlightId}"

    fun flightDetails(flightId: String): String = "flight/$flightId"
}

