package com.example.skywatch.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "flight_sightings",
    indices = [
        Index(value = ["flightId"]),
        Index(value = ["detectedAtEpochMillis"]),
    ],
)
data class FlightSightingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val detectedAtEpochMillis: Long,
    val flightId: String,
    val callsign: String?,
    val originCountry: String?,
    val latitude: Double?,
    val longitude: Double?,
    val altitudeMeters: Double?,
    val speedMs: Double?,
    val heading: Double?,
    val isWatchlisted: Boolean,
)

