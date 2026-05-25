package com.example.skywatch.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        FlightSightingEntity::class,
        WatchlistEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class SkyWatchDatabase : RoomDatabase() {
    abstract fun flightSightingDao(): FlightSightingDao
    abstract fun watchlistDao(): WatchlistDao

    companion object {
        @Volatile private var instance: SkyWatchDatabase? = null

        fun get(appContext: Context): SkyWatchDatabase {
            return instance ?: synchronized(this) {
                instance
                    ?: Room.databaseBuilder(appContext, SkyWatchDatabase::class.java, "skywatch.db")
                        .fallbackToDestructiveMigration(dropAllTables = true)
                        .build()
                        .also { instance = it }
            }
        }
    }
}

