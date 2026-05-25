package com.example.skywatch.tracking

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.skywatch.R
import com.example.skywatch.data.db.SkyWatchDatabase
import com.example.skywatch.data.opensky.OpenSkyClient
import com.example.skywatch.data.opensky.OpenSkyRepository
import com.example.skywatch.data.prefs.LocationMode
import com.example.skywatch.data.prefs.UserSettingsRepository
import com.example.skywatch.data.watchlist.WatchlistRepository
import com.example.skywatch.domain.NotificationRateLimiter
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.example.skywatch.notifications.FlightAlertNotifier
import com.example.skywatch.data.db.FlightSightingEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDateTime
import java.time.LocalDate
import java.time.LocalTime

/**
 * Foreground service that polls OpenSky every ~15–30 seconds while tracking is ON.
 */
class TrackingService : Service() {
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob + Dispatchers.IO)

    private var pollingJob: Job? = null

    private val settingsRepo by lazy { UserSettingsRepository(applicationContext) }
    private val openSkyRepo by lazy { OpenSkyRepository(OpenSkyClient.createApi()) }
    private val db by lazy { SkyWatchDatabase.get(applicationContext) }
    private val watchlistRepo by lazy { WatchlistRepository(db.watchlistDao()) }
    private val flightNotifier by lazy { FlightAlertNotifier(applicationContext).also { it.ensureChannels() } }
    private val rateLimiter = NotificationRateLimiter()

    // flightId -> lastSeenMillis (used for dedupe)
    private val lastSeenByFlightId: MutableMap<String, Long> = LinkedHashMap()

    override fun onCreate() {
        super.onCreate()
        ensureTrackingChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            Actions.Start -> startTrackingIfNeeded()
            Actions.Stop -> stopTracking()
            else -> startTrackingIfNeeded()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        pollingJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startTrackingIfNeeded() {
        if (pollingJob?.isActive == true) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NotificationIds.TrackingStatus,
                buildTrackingStatusNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NotificationIds.TrackingStatus, buildTrackingStatusNotification())
        }

        pollingJob =
            serviceScope.launch {
                settingsRepo.setTrackingEnabled(true)
                settingsRepo.updateStreakForToday(LocalDate.now().toEpochDay())

                while (isActive) {
                    val settings = settingsRepo.settings.first()

                    if (!isWithinActiveWindow(settings.activeFromMinutes, settings.activeToMinutes)) {
                        delay(PollingInactiveDelayMs)
                        continue
                    }

                    val center = resolveTrackingCenter(settings)
                    if (center == null) {
                        delay(PollingInactiveDelayMs)
                        continue
                    }

                    // Update saved GPS location for display on UI if in GPS mode
                    if (settings.locationMode == LocationMode.CurrentGps) {
                        settingsRepo.setCurrentGpsLocation(center.lat, center.lon)
                    }

                    val nowMillis = System.currentTimeMillis()
                    try {
                        val flights =
                            openSkyRepo.fetchFlightsInRadius(
                                centerLat = center.lat,
                                centerLon = center.lon,
                                radiusKm = settings.trackingRadiusKm,
                                detectedAtEpochMillis = nowMillis,
                            )

                        // Dedupe baseline (actual notifications are implemented in next todo).
                        val newlySeen =
                            flights.filter { f ->
                                val lastSeen = lastSeenByFlightId[f.flightId]
                                lastSeen == null || (nowMillis - lastSeen) > DedupeWindowMs
                            }
                        flights.forEach { f -> lastSeenByFlightId[f.flightId] = nowMillis }

                        if (newlySeen.isNotEmpty()) {
                            val watchlist = watchlistRepo.getCallsignSetUppercase()
                            val locationLabel =
                                if (settings.locationMode == LocationMode.Manual) settings.manualLabel else "your area"

                            val prioritized =
                                newlySeen.sortedWith(
                                    compareByDescending<com.example.skywatch.domain.model.Flight> { f ->
                                        val cs = f.callsign?.trim()?.uppercase()
                                        cs != null && watchlist.contains(cs)
                                    }.thenBy { it.callsign ?: it.flightId }
                                )

                            // Track all newly seen flights within the radius (priority flights are sorted to the top).
                            val flightsToTrack = prioritized

                            if (flightsToTrack.isNotEmpty()) {
                                // Persist to local history (7-day rolling).
                                val dao = db.flightSightingDao()
                                flightsToTrack.forEach { flight ->
                                    val cs = flight.callsign?.trim()?.uppercase()
                                    val isWatchlisted = cs != null && watchlist.contains(cs)
                                    dao.insert(
                                        FlightSightingEntity(
                                            detectedAtEpochMillis = flight.detectedAtEpochMillis,
                                            flightId = flight.flightId,
                                            callsign = flight.callsign,
                                            originCountry = flight.originCountry,
                                            latitude = flight.latitude,
                                            longitude = flight.longitude,
                                            altitudeMeters = flight.altitudeMeters,
                                            speedMs = flight.speedMs,
                                            heading = flight.heading,
                                            isWatchlisted = isWatchlisted,
                                        )
                                    )
                                }
                                dao.deleteOlderThan(nowMillis - HistoryRetentionMs)

                                postFlightNotifications(
                                    flights = flightsToTrack,
                                    watchlist = watchlist,
                                    locationLabel = locationLabel,
                                    soundEnabled = settings.soundEnabled,
                                    nowMillis = nowMillis,
                                    maxPerMinute = settings.maxNotificationsPerMinute,
                                )
                            }
                        }
                    } catch (_: Throwable) {
                        // Network failures / rate limits: just retry on next tick.
                    }

                    delay(PollingIntervalMs)
                }
            }
    }

    private fun stopTracking() {
        pollingJob?.cancel()
        pollingJob = null

        serviceScope.launch {
            settingsRepo.setTrackingEnabled(false)
            settingsRepo.setCurrentGpsLocation(null, null)
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private suspend fun resolveTrackingCenter(settings: com.example.skywatch.data.prefs.UserSettings): CenterPoint? {
        return when (settings.locationMode) {
            LocationMode.Manual -> {
                val lat = settings.manualLat
                val lon = settings.manualLon
                if (lat != null && lon != null) CenterPoint(lat, lon) else null
            }
            LocationMode.CurrentGps -> fetchCurrentLocationCenter()
        }
    }

    private suspend fun fetchCurrentLocationCenter(): CenterPoint? {
        if (!hasFineLocationPermission()) return null

        val fused = LocationServices.getFusedLocationProviderClient(applicationContext)
        val location =
            fused.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()
        return location?.let { CenterPoint(it.latitude, it.longitude) }
    }

    private fun hasFineLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun isWithinActiveWindow(fromMinutes: Int, toMinutes: Int): Boolean {
        val now: LocalTime = LocalDateTime.now().toLocalTime()
        val nowMinutes = now.hour * 60 + now.minute

        val from = fromMinutes.coerceIn(0, 23 * 60 + 59)
        val to = toMinutes.coerceIn(0, 23 * 60 + 59)

        // PRD says “same day”, but handle wrap-around safely.
        return if (from <= to) {
            nowMinutes in from..to
        } else {
            nowMinutes >= from || nowMinutes <= to
        }
    }

    private fun ensureTrackingChannel() {
        val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel =
            NotificationChannel(
                Channels.TrackingStatus,
                "Tracking status",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Shows when SkyWatch is tracking in the background"
                setShowBadge(false)
            }
        mgr.createNotificationChannel(channel)
    }

    private fun buildTrackingStatusNotification() =
        NotificationCompat.Builder(this, Channels.TrackingStatus)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("SkyWatch is tracking…")
            .setContentText("Searching for flights near you")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(
                0,
                "Stop",
                pendingServiceIntent(Actions.Stop, requestCode = 2),
            )
            .build()

    private fun postFlightNotifications(
        flights: List<com.example.skywatch.domain.model.Flight>,
        watchlist: Set<String>,
        locationLabel: String,
        soundEnabled: Boolean,
        nowMillis: Long,
        maxPerMinute: Int,
    ) {
        if (flights.isEmpty()) return

        val available = rateLimiter.available(nowMillis, maxPerMinute)
        if (available <= 0) return

        // Special case: lots of flights but only one slot left — prefer a summary.
        if (flights.size > 3 && available == 1) {
            flightNotifier.postSummary(
                count = flights.size,
                locationLabel = locationLabel,
                soundEnabled = soundEnabled,
            )
            rateLimiter.markSent(nowMillis, 1)
            return
        }

        val individualCount = minOf(3, flights.size, available)
        flights.take(individualCount).forEach { flight ->
            val cs = flight.callsign?.trim()?.uppercase()
            val isWatchlisted = cs != null && watchlist.contains(cs)
            flightNotifier.postIndividualFlight(
                flight = flight,
                locationLabel = locationLabel,
                soundEnabled = soundEnabled,
                isWatchlisted = isWatchlisted,
            )
        }

        var sent = individualCount
        val remaining = flights.size - individualCount
        if (remaining > 0 && available > sent) {
            flightNotifier.postSummary(
                count = remaining,
                locationLabel = locationLabel,
                soundEnabled = soundEnabled,
            )
            sent += 1
        }

        rateLimiter.markSent(nowMillis, sent)
    }

    private fun pendingServiceIntent(action: String, requestCode: Int): PendingIntent {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getService(
            this,
            requestCode,
            Intent(this, TrackingService::class.java).setAction(action),
            flags,
        )
    }

    private data class CenterPoint(val lat: Double, val lon: Double)

    object Actions {
        const val Start = "com.example.skywatch.tracking.action.START"
        const val Stop = "com.example.skywatch.tracking.action.STOP"
    }

    object Channels {
        const val TrackingStatus = "tracking_status"
    }

    object NotificationIds {
        const val TrackingStatus = 1001
    }

    companion object {
        private const val PollingIntervalMs = 20_000L
        private const val PollingInactiveDelayMs = 60_000L
        private const val DedupeWindowMs = 2 * 60_000L
        private const val HistoryRetentionMs = 7L * 24 * 60 * 60 * 1000

        fun start(context: Context) {
            val intent = Intent(context, TrackingService::class.java).setAction(Actions.Start)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, TrackingService::class.java).setAction(Actions.Stop)
            context.startService(intent)
        }
    }
}
