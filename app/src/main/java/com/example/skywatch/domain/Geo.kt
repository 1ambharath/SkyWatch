package com.example.skywatch.domain

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class BoundingBox(
    val lamin: Double,
    val lomin: Double,
    val lamax: Double,
    val lomax: Double,
)

object Geo {
    private const val EarthRadiusKm = 6371.0

    fun boundingBoxKm(centerLat: Double, centerLon: Double, radiusKm: Double): BoundingBox {
        // 1 degree latitude ≈ 111 km
        val latDelta = radiusKm / 111.0
        val lonDelta = radiusKm / (111.0 * cos(centerLat * PI / 180.0))

        return BoundingBox(
            lamin = centerLat - latDelta,
            lamax = centerLat + latDelta,
            lomin = centerLon - lonDelta,
            lomax = centerLon + lonDelta,
        )
    }

    fun distanceKm(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double,
    ): Double {
        val dLat = (lat2 - lat1) * PI / 180.0
        val dLon = (lon2 - lon1) * PI / 180.0
        val rLat1 = lat1 * PI / 180.0
        val rLat2 = lat2 * PI / 180.0

        val a =
            sin(dLat / 2).pow(2.0) +
                cos(rLat1) * cos(rLat2) * sin(dLon / 2).pow(2.0)
        val c = 2.0 * asin(sqrt(min(1.0, a)))
        return EarthRadiusKm * c
    }

    fun isInRadiusKm(
        flightLat: Double?,
        flightLon: Double?,
        centerLat: Double?,
        centerLon: Double?,
        radiusKm: Double?,
    ): Boolean {
        if (flightLat == null || flightLon == null || centerLat == null || centerLon == null || radiusKm == null) {
            return false
        }
        return distanceKm(centerLat, centerLon, flightLat, flightLon) <= radiusKm
    }
}

