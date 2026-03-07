package com.grainql.sdk.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class GrainEvent(
    val eventName: String,
    val userId: String,
    val properties: Map<String, JsonElement>,
    val systemProperties: Map<String, JsonElement>,
    val timestamp: Long,
    val retryCount: Int = 0,
    val id: String,
)
