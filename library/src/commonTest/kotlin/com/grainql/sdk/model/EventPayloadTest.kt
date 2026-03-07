package com.grainql.sdk.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EventPayloadTest {

    private val json = Json { encodeDefaults = true }

    @Test
    fun serializeSingleEvent() {
        val payload = EventPayload(
            eventName = "level_complete",
            userId = "player-123",
            properties = mapOf(
                "level" to JsonPrimitive(5),
                "score" to JsonPrimitive(1250),
            ),
        )

        val serialized = json.encodeToString(payload)
        assertTrue(serialized.contains("\"eventName\":\"level_complete\""))
        assertTrue(serialized.contains("\"userId\":\"player-123\""))
        assertTrue(serialized.contains("\"level\":5"))
    }

    @Test
    fun serializeBatch() {
        val batch = listOf(
            EventPayload("event_a", "user1"),
            EventPayload("event_b", "user2", mapOf("key" to JsonPrimitive("val"))),
        )

        val serialized = json.encodeToString(batch)
        assertTrue(serialized.startsWith("["))
        assertTrue(serialized.contains("event_a"))
        assertTrue(serialized.contains("event_b"))
    }

    @Test
    fun deserializeRoundTrip() {
        val original = EventPayload(
            eventName = "test",
            userId = "u",
            properties = mapOf("\$session_id" to JsonPrimitive("abc")),
        )

        val serialized = json.encodeToString(original)
        val deserialized = json.decodeFromString<EventPayload>(serialized)
        assertEquals(original, deserialized)
    }
}
