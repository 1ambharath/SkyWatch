package com.example.skywatch.data.prefs

enum class LocationMode {
    CurrentGps,
    Manual,
}

data class UserSettings(
    val locationMode: LocationMode = LocationMode.CurrentGps,
    val manualLabel: String = "Pinned location",
    val manualLat: Double? = null,
    val manualLon: Double? = null,
    val currentGpsLat: Double? = null,
    val currentGpsLon: Double? = null,
    val trackingRadiusKm: Double = 5.0,
    val activeFromMinutes: Int = 0,
    val activeToMinutes: Int = 23 * 60 + 59,
    val maxNotificationsPerMinute: Int = 3,
    val soundEnabled: Boolean = false,
    val isTracking: Boolean = false,
    val streakCount: Int = 0,
    val lastActiveEpochDay: Long? = null,
    val lastNudgeEpochDay: Long? = null,
    val lastWeeklyResetEpochDay: Long? = null,
)
