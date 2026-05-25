package com.example.skywatch.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watchlist")
data class WatchlistEntity(
    @PrimaryKey val callsign: String,
    val createdAtEpochMillis: Long,
)

