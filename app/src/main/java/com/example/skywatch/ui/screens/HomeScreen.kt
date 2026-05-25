package com.example.skywatch.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.skywatch.data.prefs.LocationMode
import com.example.skywatch.data.prefs.UserSettings
import com.example.skywatch.data.prefs.UserSettingsRepository
import com.example.skywatch.tracking.TrackingService
import java.util.Locale

@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    onOpenFlights: () -> Unit,
) {
    val context = LocalContext.current
    val settingsRepo = remember { UserSettingsRepository(context.applicationContext) }
    val settings by settingsRepo.settings.collectAsState(initial = UserSettings())

    val requiredPermissions =
        remember {
            buildList {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
                if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
            }.toTypedArray()
        }

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
            val allGranted = grants.values.all { it }
            if (allGranted) {
                TrackingService.start(context)
            }
        }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Radar Header & Animated Scope
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            contentAlignment = Alignment.Center
        ) {
            RadarDisplay(
                isTracking = settings.isTracking,
                modifier = Modifier
                    .size(200.dp)
                    .padding(8.dp)
            )
        }

        // Active Status Indicator Badge
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (settings.isTracking) {
                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                }
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val badgeColor = if (settings.isTracking) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                }
                
                Canvas(modifier = Modifier.size(8.dp)) {
                    drawCircle(color = badgeColor)
                }
                Text(
                    text = if (settings.isTracking) "RADAR SCANNING ON" else "RADAR STANDBY",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (settings.isTracking) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }

        // Main Tracking Details
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Tracking Profile",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                val locationText = when (settings.locationMode) {
                    LocationMode.CurrentGps -> {
                        val lat = settings.currentGpsLat
                        val lon = settings.currentGpsLon
                        if (lat != null && lon != null) {
                            "Current GPS (%.4f, %.4f)".format(Locale.US, lat, lon)
                        } else {
                            "Current GPS (Waiting for fix)"
                        }
                    }
                    LocationMode.Manual -> {
                        val lat = settings.manualLat
                        val lon = settings.manualLon
                        if (lat != null && lon != null) {
                            "${settings.manualLabel} (%.4f, %.4f)".format(Locale.US, lat, lon)
                        } else {
                            settings.manualLabel
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Location Source",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = locationText,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Detection Radius",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${settings.trackingRadiusKm.toInt()} km",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Active Hours",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatActiveHours(settings.activeFromMinutes, settings.activeToMinutes),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = {
                        if (settings.isTracking) {
                            TrackingService.stop(context)
                        } else {
                            permissionLauncher.launch(requiredPermissions)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (settings.isTracking) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                ) {
                    Text(
                        text = if (settings.isTracking) "Stop Radar Tracking" else "Initiate Radar Tracking",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }

        // Streak Display Card with Amber-Red Gradient
        val streakCount = settings.streakCount
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            val gradient = if (streakCount > 0) {
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFFD97706), // Amber-600
                        Color(0xFFDC2626)  // Red-600
                    )
                )
            } else {
                Brush.horizontalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }

            Box(
                modifier = Modifier
                    .background(gradient)
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Streak System",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (streakCount > 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (streakCount > 0) {
                                "Daily tracking active!"
                            } else {
                                "Start a daily tracking streak"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (streakCount > 0) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = if (streakCount > 0) "🔥 $streakCount Days" else "💤 Off",
                        style = MaterialTheme.typography.headlineMedium,
                        color = if (streakCount > 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Navigation Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = onOpenSettings,
                modifier = Modifier.weight(1f),
            ) {
                Text("Settings")
            }
            Button(
                onClick = onOpenFlights,
                modifier = Modifier.weight(1f),
            ) {
                Text("View Flights")
            }
        }
    }
}

@Composable
fun RadarDisplay(isTracking: Boolean, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")

    val angle by if (isTracking) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(4000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "angle"
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    val pulse1 by if (isTracking) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = LinearOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "pulse1"
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    val pulse2 by if (isTracking) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, delayMillis = 1000, easing = LinearOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "pulse2"
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    val radarColor = MaterialTheme.colorScheme.secondary
    val primaryColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier) {
        val center = this.center
        val radius = this.size.minDimension / 2f

        // Draw concentric circles
        drawCircle(
            color = radarColor.copy(alpha = 0.15f),
            radius = radius * 0.3f,
            style = Stroke(width = 1.dp.toPx())
        )
        drawCircle(
            color = radarColor.copy(alpha = 0.15f),
            radius = radius * 0.6f,
            style = Stroke(width = 1.dp.toPx())
        )
        drawCircle(
            color = radarColor.copy(alpha = 0.2f),
            radius = radius,
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Draw radar grid lines
        drawLine(
            color = radarColor.copy(alpha = 0.1f),
            start = Offset(center.x - radius, center.y),
            end = Offset(center.x + radius, center.y),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = radarColor.copy(alpha = 0.1f),
            start = Offset(center.x, center.y - radius),
            end = Offset(center.x, center.y + radius),
            strokeWidth = 1.dp.toPx()
        )

        if (isTracking) {
            // Draw pulses
            drawCircle(
                color = primaryColor.copy(alpha = 0.15f * (1f - pulse1)),
                radius = radius * pulse1,
                style = Stroke(width = 3.dp.toPx())
            )
            drawCircle(
                color = primaryColor.copy(alpha = 0.15f * (1f - pulse2)),
                radius = radius * pulse2,
                style = Stroke(width = 3.dp.toPx())
            )

            // Draw scanning sweep
            val sweepGradient = Brush.sweepGradient(
                colors = listOf(
                    radarColor.copy(alpha = 0f),
                    radarColor.copy(alpha = 0.4f)
                ),
                center = center
            )

            drawArc(
                brush = sweepGradient,
                startAngle = angle - 45f,
                sweepAngle = 45f,
                useCenter = true,
                size = this.size
            )
        }
    }
}

private fun formatActiveHours(fromMinutes: Int, toMinutes: Int): String {
    fun fmt(m: Int): String {
        val h = (m / 60).coerceIn(0, 23)
        val min = (m % 60).coerceIn(0, 59)
        return "%02d:%02d".format(h, min)
    }
    return if (fromMinutes == 0 && toMinutes == 23 * 60 + 59) "All day" else "${fmt(fromMinutes)}–${fmt(toMinutes)}"
}
