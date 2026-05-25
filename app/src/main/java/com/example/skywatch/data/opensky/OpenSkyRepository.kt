package com.example.skywatch.data.opensky

import com.example.skywatch.domain.BoundingBox
import com.example.skywatch.domain.Geo
import com.example.skywatch.domain.model.Flight

class OpenSkyRepository(
    private val api: OpenSkyApi,
) {
    suspend fun fetchFlightsInRadius(
        centerLat: Double,
        centerLon: Double,
        radiusKm: Double,
        detectedAtEpochMillis: Long,
    ): List<Flight> {
        val bbox: BoundingBox = Geo.boundingBoxKm(centerLat, centerLon, radiusKm)
        val response = api.getStates(bbox.lamin, bbox.lomin, bbox.lamax, bbox.lomax)
        val all = OpenSkyMapper.toFlights(response, detectedAtEpochMillis)
        return all.filter { f ->
            Geo.isInRadiusKm(
                flightLat = f.latitude,
                flightLon = f.longitude,
                centerLat = centerLat,
                centerLon = centerLon,
                radiusKm = radiusKm,
            )
        }
    }
}

