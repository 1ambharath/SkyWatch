package com.example.skywatch.data.watchlist

import com.example.skywatch.data.db.WatchlistDao
import com.example.skywatch.data.db.WatchlistEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WatchlistRepository(
    private val dao: WatchlistDao,
) {
    val watchlist: Flow<List<String>> =
        dao.observeWatchlist().map { list -> list.map { it.callsign } }

    suspend fun getCallsignSetUppercase(): Set<String> =
        dao.getCallsigns().map { it.trim().uppercase() }.filter { it.isNotEmpty() }.toSet()

    suspend fun addCallsign(callsignRaw: String): Boolean {
        val callsign = callsignRaw.trim().uppercase()
        if (callsign.isEmpty()) return false
        if (dao.count() >= MaxItems) return false
        dao.upsert(
            WatchlistEntity(
                callsign = callsign,
                createdAtEpochMillis = System.currentTimeMillis(),
            )
        )
        return true
    }

    suspend fun removeCallsign(callsign: String) {
        dao.delete(callsign.trim().uppercase())
    }

    companion object {
        const val MaxItems = 10
    }
}

