package com.example.skywatch.domain

import kotlin.math.roundToInt

object Formatters {
    fun altitudeFeet(altitudeMeters: Double?): String {
        if (altitudeMeters == null) return "Unknown"
        val feet = altitudeMeters * 3.28084
        return "${feet.roundToInt()} ft"
    }

    fun speedKmh(speedMs: Double?): String {
        if (speedMs == null) return "Unknown"
        val kmh = speedMs * 3.6
        return "${kmh.roundToInt()} km/h"
    }
}

