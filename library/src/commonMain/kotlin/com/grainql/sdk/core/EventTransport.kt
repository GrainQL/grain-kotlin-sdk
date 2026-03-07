package com.grainql.sdk.core

import com.grainql.sdk.GrainConfig
import com.grainql.sdk.model.EventPayload
import co.touchlab.kermit.Logger
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

internal class EventTransport(private val config: GrainConfig) {

    private val log = Logger.withTag("GrainTransport")

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
    }

    suspend fun sendBatch(events: List<EventPayload>): TransportResult {
        if (events.isEmpty()) return TransportResult.Success

        val url = "${config.apiUrl}/v1/events/${config.tenantAlias}/batch"

        return try {
            val response: HttpResponse = client.post(url) {
                contentType(ContentType.Application.Json)
                config.secret?.let { header("Authorization", "Chase $it") }
                setBody(events)
            }

            when (response.status.value) {
                200 -> TransportResult.Success
                401 -> {
                    log.e { "Authentication failed (401). Check your tenant secret." }
                    TransportResult.AuthError
                }
                413 -> {
                    log.e { "Batch too large (413). Events: ${events.size}" }
                    TransportResult.BatchTooLarge
                }
                503 -> {
                    log.w { "Server overloaded (503). Will retry." }
                    TransportResult.ServerOverloaded
                }
                else -> {
                    log.w { "Unexpected response: ${response.status}" }
                    TransportResult.RetryableError
                }
            }
        } catch (e: Exception) {
            log.w(e) { "Network error sending batch" }
            TransportResult.RetryableError
        }
    }

    suspend fun sendUserProperties(userId: String, properties: Map<String, String>) {
        val url = "${config.apiUrl}/v1/events/${config.tenantAlias}/properties"

        try {
            val body = buildMap {
                put("userId", userId)
                putAll(properties)
            }
            val response: HttpResponse = client.post(url) {
                contentType(ContentType.Application.Json)
                config.secret?.let { header("Authorization", "Chase $it") }
                setBody(body)
            }
            if (response.status.value != 200) {
                log.w { "Failed to set user properties: ${response.status}" }
            }
        } catch (e: Exception) {
            log.w(e) { "Error sending user properties" }
        }
    }

    fun close() {
        client.close()
    }
}

internal enum class TransportResult {
    Success,
    AuthError,
    BatchTooLarge,
    ServerOverloaded,
    RetryableError,
}
