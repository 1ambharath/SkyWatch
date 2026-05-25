package com.example.skywatch.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.skywatch.data.db.SkyWatchDatabase
import com.example.skywatch.domain.Formatters
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private data class FlightRow(
    val flightId: String,
    val callsign: String,
    val altitude: String,
    val time: String,
    val isWatchlisted: Boolean,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlightListScreen(
    onNavigateBack: () -> Unit,
    onOpenFlight: (flightId: String) -> Unit,
) {
    val context = LocalContext.current
    val dao = remember { SkyWatchDatabase.get(context.applicationContext).flightSightingDao() }
    val sightings by dao.observeRecent(limit = 500).collectAsState(initial = emptyList())

    val zone = ZoneId.systemDefault()
    val grouped: Map<LocalDate, List<FlightRow>> =
        sightings
            .map { e ->
                val dt = Instant.ofEpochMilli(e.detectedAtEpochMillis).atZone(zone)
                val date = dt.toLocalDate()
                val time = timeFormatter.format(dt)
                val callsign = e.callsign ?: e.flightId
                date to
                    FlightRow(
                        flightId = e.flightId,
                        callsign = callsign,
                        altitude = Formatters.altitudeFeet(e.altitudeMeters),
                        time = time,
                        isWatchlisted = e.isWatchlisted,
                    )
            }
            .groupBy(keySelector = { it.first }, valueTransform = { it.second })

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detected Flights") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (grouped.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No flights detected yet.\nStart radar tracking to monitor skies.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                val dates = grouped.keys.sortedDescending()
                dates.forEach { date ->
                    item(key = "header-$date") {
                        Text(
                            text = dateFormatter.format(date),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 16.dp, bottom = 6.dp),
                        )
                    }
                    flightRows(
                        rows = grouped[date].orEmpty(),
                        onOpenFlight = onOpenFlight,
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

private fun LazyListScope.flightRows(
    rows: List<FlightRow>,
    onOpenFlight: (flightId: String) -> Unit,
) {
    items(rows, key = { "${it.flightId}-${it.time}" }) { flight ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenFlight(flight.flightId) },
            colors = CardDefaults.cardColors(
                containerColor = if (flight.isWatchlisted) {
                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f)
                } else {
                    MaterialTheme.colorScheme.surface
                }
            ),
            border = if (flight.isWatchlisted) {
                BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f))
            } else {
                null
            }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Leading Icon status
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (flight.isWatchlisted) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Watchlisted Flight",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(28.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Flight,
                            contentDescription = "General Flight",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Call sign and details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = flight.callsign,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (flight.isWatchlisted) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    Text(
                        text = "Altitude: ${flight.altitude}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Time and trailing arrow
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = flight.time,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "More details",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d")
