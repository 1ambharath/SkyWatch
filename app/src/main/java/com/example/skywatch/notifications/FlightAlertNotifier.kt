package com.example.skywatch.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.skywatch.MainActivity
import com.example.skywatch.R
import com.example.skywatch.domain.model.Flight
import com.example.skywatch.ui.navigation.NavIntents
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class FlightAlertNotifier(
    private val appContext: Context,
) {
    private val mgr = NotificationManagerCompat.from(appContext)

    fun ensureChannels() {
        val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val silent =
            NotificationChannel(
                Channels.FlightAlertsSilent,
                "Flight alerts (silent)",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Flight alerts without sound"
                setShowBadge(true)
                setSound(null, null)
                enableVibration(false)
            }

        val sound =
            NotificationChannel(
                Channels.FlightAlertsSound,
                "Flight alerts",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Flight alerts with sound"
                setShowBadge(true)
            }

        nm.createNotificationChannel(silent)
        nm.createNotificationChannel(sound)
    }

    fun postIndividualFlight(
        flight: Flight,
        locationLabel: String,
        soundEnabled: Boolean,
        isWatchlisted: Boolean,
    ) {
        val channelId = if (soundEnabled) Channels.FlightAlertsSound else Channels.FlightAlertsSilent
        val title = flight.callsign ?: flight.flightId
        val detectedAt = formatTime(flight.detectedAtEpochMillis)

        val content = "Detected at $detectedAt • over $locationLabel"

        val contentIntent = pendingActivityIntentForFlightDetails(flight.flightId)
        val detailsIntent = pendingActivityIntentForFlightDetails(flight.flightId)

        val notification =
            NotificationCompat.Builder(appContext, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(if (isWatchlisted) "Tracked: $title" else title)
                .setContentText(content)
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setGroup(GroupKey)
                .addAction(0, "More Details", detailsIntent)
                .build()

        mgr.notify(notificationIdForFlight(flight.flightId), notification)
    }

    fun postSummary(
        count: Int,
        locationLabel: String,
        soundEnabled: Boolean,
    ) {
        val channelId = if (soundEnabled) Channels.FlightAlertsSound else Channels.FlightAlertsSilent
        val title = "$count more planes passed"
        val text = "Tap to see all near $locationLabel"

        val intent = pendingActivityIntentForFlights()

        val notification =
            NotificationCompat.Builder(appContext, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(intent)
                .setAutoCancel(true)
                .setGroup(GroupKey)
                .setGroupSummary(true)
                .build()

        mgr.notify(NotificationIds.FlightSummary, notification)
    }

    private fun pendingActivityIntentForFlights(): PendingIntent {
        val intent =
            Intent(appContext, MainActivity::class.java)
                .putExtra(NavIntents.ExtraNavTarget, NavIntents.TargetFlights)

        return PendingIntent.getActivity(
            appContext,
            5001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun pendingActivityIntentForFlightDetails(flightId: String): PendingIntent {
        val intent =
            Intent(appContext, MainActivity::class.java)
                .putExtra(NavIntents.ExtraNavTarget, NavIntents.TargetFlightDetails)
                .putExtra(NavIntents.ExtraFlightId, flightId)

        return PendingIntent.getActivity(
            appContext,
            5002,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun notificationIdForFlight(flightId: String): Int =
        (flightId.hashCode() and 0x7fffffff) + 10_000

    private fun formatTime(epochMillis: Long): String =
        timeFormatter.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))

    companion object {
        private const val GroupKey = "skywatch_flight_alerts"

        private val timeFormatter: DateTimeFormatter =
            DateTimeFormatter.ofPattern("HH:mm")
    }

    object Channels {
        const val FlightAlertsSilent = "flight_alerts_silent"
        const val FlightAlertsSound = "flight_alerts_sound"
    }

    object NotificationIds {
        const val FlightSummary = 2001
    }
}

