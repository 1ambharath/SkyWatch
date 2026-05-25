package com.example.skywatch.map

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline

/**
 * Lightweight OSMDroid map wrapper for Compose.
 *
 * Shows a centered map with an optional trajectory polyline.
 */
@SuppressLint("ClickableViewAccessibility")
@Composable
fun FlightMap(
    centerLat: Double,
    centerLon: Double,
    trajectory: List<GeoPoint>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    // Keep a single MapView instance across recompositions.
    val mapView =
        remember {
            MapView(context).apply {
                id = android.R.id.custom
                layoutParams =
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(10.0)
            }
        }

    DisposableEffect(Unit) {
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { mapView },
        update = { map ->
            val center = GeoPoint(centerLat, centerLon)
            map.controller.setCenter(center)

            // Clear previous trajectory overlays (keep base layer).
            map.overlays.removeAll { it is Polyline }

            if (trajectory.size >= 2) {
                val line = Polyline(map)
                line.setPoints(trajectory)
                map.overlays.add(line)
            }

            map.invalidate()
        },
    )
}

