package com.example.skywatch.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchlistDao {
    @Query("SELECT * FROM watchlist ORDER BY createdAtEpochMillis DESC")
    fun observeWatchlist(): Flow<List<WatchlistEntity>>

    @Query("SELECT callsign FROM watchlist")
    suspend fun getCallsigns(): List<String>

    @Query("SELECT COUNT(*) FROM watchlist")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: WatchlistEntity)

    @Query("DELETE FROM watchlist WHERE callsign = :callsign")
    suspend fun delete(callsign: String): Int
}

