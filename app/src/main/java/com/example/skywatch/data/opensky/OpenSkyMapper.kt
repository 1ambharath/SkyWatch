package com.example.skywatch.data.opensky

import com.example.skywatch.domain.model.Flight

object OpenSkyMapper {
    /**
     * OpenSky `states` array mapping indices (per PRD):
     * 0: flightId (icao24)
     * 1: callsign
     * 2: originCountry
     * 5: longitude
     * 6: latitude
     * 7: altitude (meters)
     * 8: onGround
     * 9: speed (m/s)
     * 10: heading (degrees)
     */
    fun toFlights(
        response: OpenSkyStatesResponse,
        detectedAtEpochMillis: Long,
    ): List<Flight> {
        val states = response.states ?: return emptyList()
        return states.mapNotNull { row -> toFlightOrNull(row, detectedAtEpochMillis) }
    }

    private fun toFlightOrNull(row: List<Any?>, detectedAtEpochMillis: Long): Flight? {
        val flightId = row.getOrNull(0) as? String ?: return null
        val callsign = (row.getOrNull(1) as? String)?.trim()?.takeIf { it.isNotEmpty() }
        val originCountry = row.getOrNull(2) as? String

        val longitude = row.getOrNull(5).asDoubleOrNull()
        val latitude = row.getOrNull(6).asDoubleOrNull()
        val altitudeMeters = row.getOrNull(7).asDoubleOrNull()
        val onGround = row.getOrNull(8) as? Boolean
        val speedMs = row.getOrNull(9).asDoubleOrNull()
        val heading = row.getOrNull(10).asDoubleOrNull()

        return Flight(
            flightId = flightId,
            callsign = callsign,
            originCountry = originCountry,
            longitude = longitude,
            latitude = latitude,
            altitudeMeters = altitudeMeters,
            onGround = onGround,
            speedMs = speedMs,
            heading = heading,
            detectedAtEpochMillis = detectedAtEpochMillis,
        )
    }

    private fun Any?.asDoubleOrNull(): Double? {
        return when (this) {
            is Double -> this
            is Float -> this.toDouble()
            is Int -> this.toDouble()
            is Long -> this.toDouble()
            is Number -> this.toDouble()
            is String -> this.toDoubleOrNull()
            else -> null
        }
    }
}

