package com.example.skywatch.domain.model

data class Flight(
    val flightId: String,
    val callsign: String?,
    val originCountry: String?,
    val longitude: Double?,
    val latitude: Double?,
    val altitudeMeters: Double?,
    val onGround: Boolean?,
    val speedMs: Double?,
    val heading: Double?,
    val detectedAtEpochMillis: Long,
)

