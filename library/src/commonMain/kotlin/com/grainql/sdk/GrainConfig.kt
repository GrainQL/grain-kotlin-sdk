package com.grainql.sdk

/**
 * Configuration for the Grain SDK.
 *
 * @property tenantAlias Tenant alias that identifies your project on Grain (e.g. `"my-app"`).
 *   This is the route key used in all API calls.
 * @property apiUrl Base URL for the Grain client API. Override for self-hosted deployments.
 * @property secret Optional tenant secret for authenticated ingestion (`Chase` scheme).
 *   Leave `null` for tenants with auth strategy set to `NONE`.
 * @property debug When `true`, the SDK logs tracking calls, flush activity, and identity changes.
 * @property flushIntervalMs How often (in milliseconds) the SDK automatically flushes queued events.
 *   Defaults to 30 seconds — longer than the web SDK's 5s, optimized for mobile battery.
 * @property flushThreshold Number of queued events that triggers an immediate flush,
 *   without waiting for the periodic timer.
 * @property maxBatchSize Maximum number of events sent in a single HTTP request.
 *   Must not exceed 160 (the Grain backend's hard limit).
 * @property maxQueueSize Maximum number of events held in the local queue.
 *   When exceeded, the oldest events are dropped to make room.
 * @property maxRetries Maximum retry attempts for a batch before its events are dropped.
 *   Retries use exponential backoff (1s, 2s, 4s, 8s, 16s).
 * @property enablePersistence When `true` and a [com.grainql.sdk.platform.FileStore] is provided,
 *   events are persisted to disk so they survive app kills and are delivered on next launch.
 */
data class GrainConfig(
    val tenantAlias: String,
    val apiUrl: String = "https://clientapis.grainql.com",
    val secret: String? = null,
    val debug: Boolean = false,
    val flushIntervalMs: Long = 30_000L,
    val flushThreshold: Int = 25,
    val maxBatchSize: Int = 100,
    val maxQueueSize: Int = 10_000,
    val maxRetries: Int = 5,
    val enablePersistence: Boolean = true,
) {
    init {
        require(tenantAlias.isNotBlank()) { "tenantAlias must not be blank" }
        require(maxBatchSize in 1..160) { "maxBatchSize must be between 1 and 160" }
        require(flushThreshold in 1..maxBatchSize) { "flushThreshold must be between 1 and maxBatchSize" }
        require(maxQueueSize > 0) { "maxQueueSize must be positive" }
    }
}
