package com.grainql.sdk.core

import com.grainql.sdk.model.GrainEvent
import co.touchlab.kermit.Logger
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class EventQueue(private val maxSize: Int) {

    private val log = Logger.withTag("GrainQueue")
    private val mutex = Mutex()
    private val events = ArrayDeque<GrainEvent>()

    val size: Int get() = events.size

    /**
     * Enqueue an event. Returns the ID of any evicted event if the queue was full, or null.
     */
    suspend fun enqueue(event: GrainEvent): String? = mutex.withLock {
        var evictedId: String? = null
        if (events.size >= maxSize) {
            val evicted = events.removeFirst()
            evictedId = evicted.id
            log.w { "Queue full ($maxSize). Dropping oldest event: ${evicted.id}" }
        }
        events.addLast(event)
        evictedId
    }

    suspend fun peek(count: Int): List<GrainEvent> = mutex.withLock {
        events.take(count)
    }

    suspend fun remove(ids: Set<String>) = mutex.withLock {
        events.removeAll { it.id in ids }
    }

    suspend fun removeFirst(count: Int) = mutex.withLock {
        repeat(count.coerceAtMost(events.size)) {
            events.removeFirst()
        }
    }

    suspend fun drain(): List<GrainEvent> = mutex.withLock {
        val all = events.toList()
        events.clear()
        all
    }

    suspend fun incrementRetryCount(ids: Set<String>): List<GrainEvent> = mutex.withLock {
        val updated = mutableListOf<GrainEvent>()
        val mapped = events.map { event ->
            if (event.id in ids) {
                val inc = event.copy(retryCount = event.retryCount + 1)
                updated.add(inc)
                inc
            } else event
        }
        events.clear()
        events.addAll(mapped)
        updated
    }

    suspend fun restoreAll(restored: List<GrainEvent>) = mutex.withLock {
        for (event in restored) {
            if (events.size < maxSize) {
                events.addLast(event)
            }
        }
    }
}
