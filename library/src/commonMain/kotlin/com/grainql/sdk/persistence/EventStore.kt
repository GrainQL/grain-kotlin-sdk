package com.grainql.sdk.persistence

import com.grainql.sdk.model.GrainEvent
import com.grainql.sdk.platform.FileStore
import co.touchlab.kermit.Logger
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class EventStore(private val fileStore: FileStore) {

    private val log = Logger.withTag("GrainStore")
    private val mutex = Mutex()

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    suspend fun save(events: List<GrainEvent>) = mutex.withLock {
        if (events.isEmpty()) return@withLock
        try {
            val newLines = events.joinToString("\n") { json.encodeToString(it) }
            val existing = fileStore.read(FILENAME)
            val content = if (existing.isNullOrBlank()) newLines else "$existing\n$newLines"
            fileStore.write(FILENAME, content)
        } catch (e: Exception) {
            log.w(e) { "Failed to persist events" }
        }
    }

    suspend fun loadAll(): List<GrainEvent> = mutex.withLock {
        try {
            loadFromDisk()
        } catch (e: Exception) {
            log.w(e) { "Failed to load persisted events" }
            emptyList()
        }
    }

    suspend fun remove(ids: Set<String>) = mutex.withLock {
        try {
            val existing = loadFromDisk()
            val filtered = existing.filter { it.id !in ids }
            writeToDisk(filtered)
        } catch (e: Exception) {
            log.w(e) { "Failed to remove persisted events" }
        }
    }

    suspend fun update(events: List<GrainEvent>) = mutex.withLock {
        if (events.isEmpty()) return@withLock
        try {
            val idToEvent = events.associateBy { it.id }
            val existing = loadFromDisk()
            val merged = existing.map { idToEvent[it.id] ?: it }
            writeToDisk(merged)
        } catch (e: Exception) {
            log.w(e) { "Failed to update persisted events" }
        }
    }

    suspend fun clear() = mutex.withLock {
        try {
            fileStore.delete(FILENAME)
        } catch (e: Exception) {
            log.w(e) { "Failed to clear persisted events" }
        }
    }

    private fun loadFromDisk(): List<GrainEvent> {
        val content = fileStore.read(FILENAME) ?: return emptyList()
        if (content.isBlank()) return emptyList()
        return content.lines()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                try {
                    json.decodeFromString<GrainEvent>(line)
                } catch (e: Exception) {
                    log.w { "Skipping corrupt event line" }
                    null
                }
            }
    }

    private fun writeToDisk(events: List<GrainEvent>) {
        if (events.isEmpty()) {
            fileStore.delete(FILENAME)
            return
        }
        val content = events.joinToString("\n") { json.encodeToString(it) }
        fileStore.write(FILENAME, content)
    }

    companion object {
        private const val FILENAME = "grain_events.jsonl"
    }
}
