package com.example.skywatch.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "skywatch_settings")

class UserSettingsRepository(
    private val appContext: Context,
) {
    private val store = appContext.settingsDataStore

    private object Keys {
        val locationMode = stringPreferencesKey("location_mode")
        val manualLabel = stringPreferencesKey("manual_label")
        val manualLat = doublePreferencesKey("manual_lat")
        val manualLon = doublePreferencesKey("manual_lon")
        val currentGpsLat = doublePreferencesKey("current_gps_lat")
        val currentGpsLon = doublePreferencesKey("current_gps_lon")
        val radiusKm = doublePreferencesKey("tracking_radius_km")
        val activeFromMin = intPreferencesKey("active_from_minutes")
        val activeToMin = intPreferencesKey("active_to_minutes")
        val maxNotifPerMin = intPreferencesKey("max_notif_per_min")
        val soundEnabled = booleanPreferencesKey("sound_enabled")
        val isTracking = booleanPreferencesKey("is_tracking")
        val streakCount = intPreferencesKey("streak_count")
        val lastActiveEpochDay = longPreferencesKey("last_active_epoch_day")
        val lastNudgeEpochDay = longPreferencesKey("last_nudge_epoch_day")
        val lastWeeklyResetEpochDay = longPreferencesKey("last_weekly_reset_epoch_day")
    }

    val settings: Flow<UserSettings> =
        store.data.map { prefs ->
            val mode =
                when (prefs[Keys.locationMode]) {
                    LocationMode.Manual.name -> LocationMode.Manual
                    else -> LocationMode.CurrentGps
                }

            val lastActive = prefs[Keys.lastActiveEpochDay]
            val rawStreak = prefs[Keys.streakCount] ?: 0
            val todayEpochDay = java.time.LocalDate.now().toEpochDay()
            // Reset streak to 0 dynamically if last active was before yesterday
            val streak = if (lastActive != null && todayEpochDay > lastActive + 1) 0 else rawStreak

            UserSettings(
                locationMode = mode,
                manualLabel = prefs[Keys.manualLabel] ?: "Pinned location",
                manualLat = prefs[Keys.manualLat],
                manualLon = prefs[Keys.manualLon],
                currentGpsLat = prefs[Keys.currentGpsLat],
                currentGpsLon = prefs[Keys.currentGpsLon],
                trackingRadiusKm = prefs[Keys.radiusKm] ?: 5.0,
                activeFromMinutes = prefs[Keys.activeFromMin] ?: 0,
                activeToMinutes = prefs[Keys.activeToMin] ?: (23 * 60 + 59),
                maxNotificationsPerMinute = prefs[Keys.maxNotifPerMin] ?: 3,
                soundEnabled = prefs[Keys.soundEnabled] ?: false,
                isTracking = prefs[Keys.isTracking] ?: false,
                streakCount = streak,
                lastActiveEpochDay = lastActive,
                lastNudgeEpochDay = prefs[Keys.lastNudgeEpochDay],
                lastWeeklyResetEpochDay = prefs[Keys.lastWeeklyResetEpochDay],
            )
        }

    suspend fun updateAllSettings(
        radiusKm: Double,
        activeFromMinutes: Int,
        activeToMinutes: Int,
        maxNotificationsPerMinute: Int,
        soundEnabled: Boolean,
        locationMode: LocationMode,
        manualLabel: String,
        manualLat: Double?,
        manualLon: Double?
    ) {
        store.edit { prefs ->
            prefs[Keys.radiusKm] = radiusKm
            prefs[Keys.activeFromMin] = activeFromMinutes.coerceIn(0, 23 * 60 + 59)
            prefs[Keys.activeToMin] = activeToMinutes.coerceIn(0, 23 * 60 + 59)
            prefs[Keys.maxNotifPerMin] = maxNotificationsPerMinute.coerceIn(1, 20)
            prefs[Keys.soundEnabled] = soundEnabled
            prefs[Keys.locationMode] = locationMode.name
            prefs[Keys.manualLabel] = manualLabel
            if (manualLat != null) prefs[Keys.manualLat] = manualLat else prefs.remove(Keys.manualLat)
            if (manualLon != null) prefs[Keys.manualLon] = manualLon else prefs.remove(Keys.manualLon)
        }
    }

    suspend fun setTrackingEnabled(enabled: Boolean) {
        store.edit { it[Keys.isTracking] = enabled }
    }

    suspend fun setCurrentGpsLocation(lat: Double?, lon: Double?) {
        store.edit { prefs ->
            if (lat == null || lon == null) {
                prefs.remove(Keys.currentGpsLat)
                prefs.remove(Keys.currentGpsLon)
            } else {
                prefs[Keys.currentGpsLat] = lat
                prefs[Keys.currentGpsLon] = lon
            }
        }
    }

    suspend fun setRadiusKm(radiusKm: Double) {
        store.edit { it[Keys.radiusKm] = radiusKm }
    }

    suspend fun setActiveHours(fromMinutes: Int, toMinutes: Int) {
        store.edit {
            it[Keys.activeFromMin] = fromMinutes.coerceIn(0, 23 * 60 + 59)
            it[Keys.activeToMin] = toMinutes.coerceIn(0, 23 * 60 + 59)
        }
    }

    suspend fun setMaxNotificationsPerMinute(max: Int) {
        store.edit { it[Keys.maxNotifPerMin] = max.coerceIn(1, 20) }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        store.edit { it[Keys.soundEnabled] = enabled }
    }

    suspend fun setLocationMode(mode: LocationMode) {
        store.edit { it[Keys.locationMode] = mode.name }
    }

    suspend fun setManualLocation(label: String, lat: Double?, lon: Double?) {
        store.edit { prefs ->
            prefs[Keys.manualLabel] = label
            if (lat != null) prefs[Keys.manualLat] = lat else prefs.remove(Keys.manualLat)
            if (lon != null) prefs[Keys.manualLon] = lon else prefs.remove(Keys.manualLon)
        }
    }

    suspend fun updateStreakForToday(todayEpochDay: Long) {
        store.edit { prefs ->
            val lastActive = prefs[Keys.lastActiveEpochDay]
            val currentStreak = prefs[Keys.streakCount] ?: 0

            val newStreak =
                when {
                    lastActive == null -> 1
                    lastActive == todayEpochDay -> currentStreak.coerceAtLeast(1)
                    lastActive == todayEpochDay - 1 -> (currentStreak + 1).coerceAtLeast(1)
                    else -> 1
                }

            prefs[Keys.streakCount] = newStreak
            prefs[Keys.lastActiveEpochDay] = todayEpochDay
        }
    }

    suspend fun setLastNudgeEpochDay(todayEpochDay: Long) {
        store.edit { it[Keys.lastNudgeEpochDay] = todayEpochDay }
    }

    suspend fun setLastWeeklyResetEpochDay(todayEpochDay: Long) {
        store.edit { it[Keys.lastWeeklyResetEpochDay] = todayEpochDay }
    }

    suspend fun resetStreak() {
        store.edit { prefs ->
            prefs[Keys.streakCount] = 0
            prefs.remove(Keys.lastActiveEpochDay)
        }
    }
}
