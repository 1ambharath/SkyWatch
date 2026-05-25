package com.example.skywatch.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.skywatch.data.db.SkyWatchDatabase
import com.example.skywatch.data.prefs.LocationMode
import com.example.skywatch.data.prefs.UserSettings
import com.example.skywatch.data.prefs.UserSettingsRepository
import com.example.skywatch.data.watchlist.WatchlistRepository
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val repo = remember { UserSettingsRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()
    val settings by repo.settings.collectAsState(initial = UserSettings())

    val db = remember { SkyWatchDatabase.get(context.applicationContext) }
    val watchlistRepo = remember { WatchlistRepository(db.watchlistDao()) }
    val watchlist by watchlistRepo.watchlist.collectAsState(initial = emptyList())

    var radiusKm by remember(settings.trackingRadiusKm) { mutableDoubleStateOf(settings.trackingRadiusKm) }
    var activeFrom by remember(settings.activeFromMinutes) { mutableStateOf(formatMinutes(settings.activeFromMinutes)) }
    var activeTo by remember(settings.activeToMinutes) { mutableStateOf(formatMinutes(settings.activeToMinutes)) }
    var notifPerMin by remember(settings.maxNotificationsPerMinute) { mutableStateOf(settings.maxNotificationsPerMinute.toString()) }
    var soundEnabled by remember(settings.soundEnabled) { mutableStateOf(settings.soundEnabled) }
    var locationMode by remember(settings.locationMode) { mutableStateOf(settings.locationMode) }
    var manualLabel by remember(settings.manualLabel) { mutableStateOf(settings.manualLabel) }
    var manualLatText by remember(settings.manualLat) { mutableStateOf(settings.manualLat?.toString().orEmpty()) }
    var manualLonText by remember(settings.manualLon) { mutableStateOf(settings.manualLon?.toString().orEmpty()) }
    var newWatchlistCallsign by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Radius Configuration Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Radar,
                        contentDescription = "Radar radius",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text("Detection Radius", style = MaterialTheme.typography.titleMedium)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Scan planes within:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${radiusKm.roundToInt()} km",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = radiusKm.toFloat(),
                    onValueChange = { radiusKm = it.toDouble() },
                    valueRange = 1f..50f,
                    steps = 48,
                )
            }
        }

        // Active Hours Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = "Active Hours",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text("Active Hours (HH:MM)", style = MaterialTheme.typography.titleMedium)
                }
                Text(
                    text = "App polls flight data only within this time window.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = activeFrom,
                        onValueChange = { activeFrom = it },
                        label = { Text("From") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    OutlinedTextField(
                        value = activeTo,
                        onValueChange = { activeTo = it },
                        label = { Text("To") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                }
            }
        }

        // Location Tracking Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location Settings",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text("Tracking Location", style = MaterialTheme.typography.titleMedium)
                }
                
                Row(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = locationMode == LocationMode.CurrentGps,
                            onClick = { locationMode = LocationMode.CurrentGps },
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Current GPS", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(
                        modifier = Modifier.weight(1.2f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = locationMode == LocationMode.Manual,
                            onClick = { locationMode = LocationMode.Manual },
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pinned location", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                if (locationMode == LocationMode.Manual) {
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = manualLabel,
                        onValueChange = { manualLabel = it },
                        label = { Text("Label (e.g. Home, Mulund)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = manualLatText,
                            onValueChange = { manualLatText = it },
                            label = { Text("Latitude") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        OutlinedTextField(
                            value = manualLonText,
                            onValueChange = { manualLonText = it },
                            label = { Text("Longitude") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                    }
                }
            }
        }

        // Notification Limit Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notification Limits",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text("Alert Caps", style = MaterialTheme.typography.titleMedium)
                }
                Text(
                    text = "Caps the number of alerts posted per minute to prevent notification spam.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = notifPerMin,
                    onValueChange = { notifPerMin = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Max notifications per minute") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
        }

        // Sound Preferences Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Sound Settings",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Alert Sound", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Play system sound for incoming alerts",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Checkbox(
                    checked = soundEnabled,
                    onCheckedChange = { soundEnabled = it },
                )
            }
        }

        // Watchlist Configuration Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FlightTakeoff,
                        contentDescription = "Watchlist",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text("Watchlist (${watchlist.size}/${WatchlistRepository.MaxItems})", style = MaterialTheme.typography.titleMedium)
                }
                Text(
                    text = "Add up to 10 specific flight numbers. These will receive priority alerts and highlighting.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = newWatchlistCallsign,
                        onValueChange = { newWatchlistCallsign = it },
                        label = { Text("e.g. AI345, SWR134") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                val added = watchlistRepo.addCallsign(newWatchlistCallsign)
                                if (added) newWatchlistCallsign = ""
                            }
                        },
                        enabled = watchlist.size < WatchlistRepository.MaxItems && newWatchlistCallsign.isNotBlank(),
                    ) {
                        Text("Add")
                    }
                }

                if (watchlist.isEmpty()) {
                    Text(
                        text = "No flights watched yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        watchlist.forEach { cs ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = cs,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                                OutlinedButton(onClick = { scope.launch { watchlistRepo.removeCallsign(cs) } }) {
                                    Text("Remove")
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Save Button
        Button(
            onClick = {
                val from = parseMinutes(activeFrom) ?: settings.activeFromMinutes
                val to = parseMinutes(activeTo) ?: settings.activeToMinutes
                val maxPerMin = notifPerMin.toIntOrNull() ?: settings.maxNotificationsPerMinute

                val manualLat = manualLatText.trim().replace(',', '.').toDoubleOrNull()
                val manualLon = manualLonText.trim().replace(',', '.').toDoubleOrNull()

                scope.launch {
                    withContext(NonCancellable) {
                        repo.updateAllSettings(
                            radiusKm = radiusKm,
                            activeFromMinutes = from,
                            activeToMinutes = to,
                            maxNotificationsPerMinute = maxPerMin,
                            soundEnabled = soundEnabled,
                            locationMode = locationMode,
                            manualLabel = manualLabel.ifBlank { "Pinned location" },
                            manualLat = manualLat,
                            manualLon = manualLon
                        )
                    }
                    onNavigateBack()
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Save Settings & Back", style = MaterialTheme.typography.titleMedium)
        }
        
        Spacer(modifier = Modifier.height(20.dp))
    }
}

private fun formatMinutes(minutes: Int): String {
    val m = minutes.coerceIn(0, 23 * 60 + 59)
    val h = m / 60
    val min = m % 60
    return "%02d:%02d".format(h, min)
}

private fun parseMinutes(text: String): Int? {
    val parts = text.trim().split(":")
    if (parts.size != 2) return null
    val h = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    if (h !in 0..23) return null
    if (m !in 0..59) return null
    return h * 60 + m
}
