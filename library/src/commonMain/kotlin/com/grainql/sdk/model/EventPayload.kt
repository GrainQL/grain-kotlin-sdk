package com.grainql.sdk.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class EventPayload(
    val eventName: String,
    val userId: String,
    val properties: Map<String, JsonElement> = emptyMap(),
)
