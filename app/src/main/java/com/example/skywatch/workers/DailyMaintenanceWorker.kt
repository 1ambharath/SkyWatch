package com.example.skywatch.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.skywatch.MainActivity
import com.example.skywatch.R
import com.example.skywatch.data.db.SkyWatchDatabase
import com.example.skywatch.data.prefs.UserSettingsRepository
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class DailyMaintenanceWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val settingsRepo = UserSettingsRepository(applicationContext)
        val db = SkyWatchDatabase.get(applicationContext)

        val nowDate = LocalDate.now()
        val todayEpochDay = nowDate.toEpochDay()
        val nowTime = LocalTime.now()

        val settings = settingsRepo.settings.first()

        // Weekly reset: Sunday midnight (best-effort, run when worker executes on Sunday).
        if (nowDate.dayOfWeek == DayOfWeek.SUNDAY && settings.lastWeeklyResetEpochDay != todayEpochDay) {
            db.flightSightingDao().deleteAll()
            settingsRepo.setLastWeeklyResetEpochDay(todayEpochDay)
        }

        // 7-day rolling retention (runs daily).
        val cutoff = System.currentTimeMillis() - HistoryRetentionMs
        db.flightSightingDao().deleteOlderThan(cutoff)

        // Streak cleanup: if the user missed yesterday and today, write 0 to the datastore.
        val lastActive = settings.lastActiveEpochDay
        val streak = settings.streakCount
        if (lastActive != null && lastActive < todayEpochDay - 1 && streak > 0) {
            settingsRepo.resetStreak()
        }

        // Streak nudge: once/day in the evening if user hasn't tracked today.
        val lastNudge = settings.lastNudgeEpochDay

        val inEveningWindow = nowTime.hour in 19..22
        val atRisk = streak > 0 && lastActive == todayEpochDay - 1
        val notAlreadyNudged = lastNudge == null || lastNudge < todayEpochDay

        if (inEveningWindow && atRisk && notAlreadyNudged) {
            ensureNudgeChannel()
            postNudgeNotification(streakCount = streak)
            settingsRepo.setLastNudgeEpochDay(todayEpochDay)
        }

        return Result.success()
    }

    private fun ensureNudgeChannel() {
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel =
            NotificationChannel(
                Channels.StreakNudge,
                "Streak nudges",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Reminders when your tracking streak is at risk"
                setShowBadge(true)
            }
        nm.createNotificationChannel(channel)
    }

    private fun postNudgeNotification(streakCount: Int) {
        val intent = Intent(applicationContext, MainActivity::class.java)
        val pending =
            PendingIntent.getActivity(
                applicationContext,
                8001,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val notification =
            NotificationCompat.Builder(applicationContext, Channels.StreakNudge)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Streak at risk")
                .setContentText("Start tracking to keep your $streakCount-day streak.")
                .setContentIntent(pending)
                .setAutoCancel(true)
                .build()

        NotificationManagerCompat.from(applicationContext).notify(NotificationIds.StreakNudge, notification)
    }

    companion object {
        private const val HistoryRetentionMs = 7L * 24 * 60 * 60 * 1000
    }

    object Channels {
        const val StreakNudge = "streak_nudge"
    }

    object NotificationIds {
        const val StreakNudge = 3001
    }
}

