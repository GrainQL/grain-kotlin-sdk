package com.grainql.sdk

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
