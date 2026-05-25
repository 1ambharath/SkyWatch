package com.example.skywatch.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FlightSightingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FlightSightingEntity): Long

    @Query("SELECT * FROM flight_sightings ORDER BY detectedAtEpochMillis DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<FlightSightingEntity>>

    @Query("SELECT * FROM flight_sightings WHERE flightId = :flightId ORDER BY detectedAtEpochMillis DESC LIMIT 1")
    suspend fun getLatestForFlight(flightId: String): FlightSightingEntity?

    @Query("SELECT * FROM flight_sightings WHERE flightId = :flightId ORDER BY detectedAtEpochMillis ASC")
    suspend fun getTrajectoryForFlight(flightId: String): List<FlightSightingEntity>

    @Query("DELETE FROM flight_sightings WHERE detectedAtEpochMillis < :cutoffEpochMillis")
    suspend fun deleteOlderThan(cutoffEpochMillis: Long): Int

    @Query("DELETE FROM flight_sightings")
    suspend fun deleteAll(): Int
}

