package com.example.skywatch.domain

import java.util.ArrayDeque

/**
 * Sliding-window limiter over the last [windowMs] milliseconds.
 *
 * Used to cap how many notifications we post per minute.
 */
class NotificationRateLimiter(
    private val windowMs: Long = 60_000L,
) {
    private val sentAt: ArrayDeque<Long> = ArrayDeque()

    @Synchronized
    fun available(nowEpochMillis: Long, maxPerWindow: Int): Int {
        cleanup(nowEpochMillis)
        return (maxPerWindow - sentAt.size).coerceAtLeast(0)
    }

    @Synchronized
    fun markSent(nowEpochMillis: Long, count: Int) {
        repeat(count.coerceAtLeast(0)) {
            sentAt.addLast(nowEpochMillis)
        }
        cleanup(nowEpochMillis)
    }

    @Synchronized
    private fun cleanup(nowEpochMillis: Long) {
        while (sentAt.isNotEmpty() && (nowEpochMillis - sentAt.first()) > windowMs) {
            sentAt.removeFirst()
        }
    }
}

