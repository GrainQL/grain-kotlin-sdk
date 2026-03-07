package com.grainql.sdk.core

import com.grainql.sdk.GrainConfig
import com.grainql.sdk.model.EventPayload
import com.grainql.sdk.model.GrainEvent
import com.grainql.sdk.persistence.EventStore
import co.touchlab.kermit.Logger
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*

internal class EventDispatcher(
    private val config: GrainConfig,
    private val queue: EventQueue,
    private val transport: EventTransport,
    private val store: EventStore?,
    private val scope: CoroutineScope,
) {
    private val log = Logger.withTag("GrainDispatcher")
    private val flushMutex = Mutex()
    private var flushJob: Job? = null
    private var networkAvailable = true
    private var consecutiveFailures = 0

    // Unbounded channel so submit() never blocks the caller
    private val pending = Channel<GrainEvent>(Channel.UNLIMITED)

    fun start() {
        startPeriodicFlush()
    }

    fun pause() {
        flushJob?.cancel()
        flushJob = null
    }

    fun resume() {
        startPeriodicFlush()
    }

    fun setNetworkAvailable(available: Boolean) {
        networkAvailable = available
        if (available) {
            consecutiveFailures = 0
            scope.launch { flush() }
        }
    }

    /**
     * Non-suspend submit — safe to call from any context.
     * Events are buffered in an unlimited channel and drained into the queue
     * before every flush.
     */
    fun submit(event: GrainEvent) {
        pending.trySend(event)
    }

    /**
     * Drain all pending events from the channel into the queue + store.
     * Called at the start of every flush to guarantee track→flush ordering.
     */
    private suspend fun drainPending() {
        while (true) {
            val event = pending.tryReceive().getOrNull() ?: break
            val evictedId = queue.enqueue(event)
            store?.save(listOf(event))
            if (evictedId != null) {
                store?.remove(setOf(evictedId))
            }
        }
    }

    fun checkThreshold() {
        if (queue.size >= config.flushThreshold) {
            scope.launch { flush() }
        }
    }

    suspend fun flush() = flushMutex.withLock {
        drainPending()

        if (!networkAvailable) {
            log.d { "Network unavailable, skipping flush" }
            return@withLock
        }

        while (queue.size > 0) {
            val batch = queue.peek(config.maxBatchSize)
            if (batch.isEmpty()) break

            val payloads = batch.map { it.toPayload() }
            val result = transport.sendBatch(payloads)

            when (result) {
                TransportResult.Success -> {
                    val ids = batch.map { it.id }.toSet()
                    queue.remove(ids)
                    store?.remove(ids)
                    consecutiveFailures = 0
                    log.d { "Flushed ${batch.size} events" }
                }
                TransportResult.AuthError -> {
                    log.e { "Auth error - clearing batch to avoid infinite retry" }
                    val ids = batch.map { it.id }.toSet()
                    queue.remove(ids)
                    store?.remove(ids)
                    break
                }
                TransportResult.BatchTooLarge -> {
                    log.e { "Batch too large - this shouldn't happen. Clearing." }
                    queue.removeFirst(batch.size)
                    break
                }
                TransportResult.ServerOverloaded, TransportResult.RetryableError -> {
                    val batchIds = batch.map { it.id }.toSet()

                    // Drop events that exceeded max retries
                    val maxRetried = batch.filter { it.retryCount >= config.maxRetries }
                    if (maxRetried.isNotEmpty()) {
                        val dropIds = maxRetried.map { it.id }.toSet()
                        queue.remove(dropIds)
                        store?.remove(dropIds)
                        log.w { "Dropped ${maxRetried.size} events after max retries" }
                    }

                    // Increment retry count for surviving events and persist
                    val survivingIds = batchIds - maxRetried.map { it.id }.toSet()
                    if (survivingIds.isNotEmpty()) {
                        val updated = queue.incrementRetryCount(survivingIds)
                        store?.update(updated)
                    }

                    // Exponential backoff: 1s, 2s, 4s, 8s, 16s (capped)
                    consecutiveFailures++
                    val backoffMs = (1000L * (1L shl (consecutiveFailures - 1).coerceAtMost(4)))
                    log.d { "Retryable error, backing off ${backoffMs}ms (attempt $consecutiveFailures)" }
                    delay(backoffMs)
                    break
                }
            }
        }
    }

    suspend fun shutdown() {
        pause()
        flush()
        transport.close()
    }

    suspend fun restorePersistedEvents() {
        val persisted = store?.loadAll() ?: return
        if (persisted.isNotEmpty()) {
            queue.restoreAll(persisted)
            log.d { "Restored ${persisted.size} persisted events" }
        }
    }

    private fun startPeriodicFlush() {
        flushJob?.cancel()
        flushJob = scope.launch {
            while (isActive) {
                delay(config.flushIntervalMs)
                try {
                    flush()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    log.w(e) { "Error during periodic flush" }
                }
            }
        }
    }

    private fun GrainEvent.toPayload(): EventPayload {
        val allProperties = buildMap<String, JsonElement> {
            putAll(properties)
            putAll(systemProperties)
        }
        return EventPayload(
            eventName = eventName,
            userId = userId,
            properties = allProperties,
        )
    }
}
