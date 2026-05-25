package com.example.skywatch

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import org.osmdroid.config.Configuration
import com.example.skywatch.workers.DailyMaintenanceWorker
import java.util.concurrent.TimeUnit

class SkyWatchApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Required by OSMDroid to avoid being blocked by tile servers.
        Configuration.getInstance().userAgentValue = packageName

        // Daily-ish housekeeping: 7-day retention, weekly reset, streak nudge.
        val request =
            PeriodicWorkRequestBuilder<DailyMaintenanceWorker>(12, TimeUnit.HOURS)
                .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "skywatch_daily_maintenance",
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }
}

