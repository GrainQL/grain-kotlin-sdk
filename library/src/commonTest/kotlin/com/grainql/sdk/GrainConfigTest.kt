package com.grainql.sdk

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GrainConfigTest {

    @Test
    fun defaultConfig() {
        val config = GrainConfig(tenantAlias = "test-tenant")
        assertEquals("test-tenant", config.tenantAlias)
        assertEquals("https://clientapis.grainql.com", config.apiUrl)
        assertEquals(30_000L, config.flushIntervalMs)
        assertEquals(25, config.flushThreshold)
        assertEquals(100, config.maxBatchSize)
        assertEquals(10_000, config.maxQueueSize)
        assertEquals(5, config.maxRetries)
        assertEquals(true, config.enablePersistence)
    }

    @Test
    fun blankAliasThrows() {
        assertFailsWith<IllegalArgumentException> {
            GrainConfig(tenantAlias = "")
        }
    }

    @Test
    fun batchSizeExceeds160Throws() {
        assertFailsWith<IllegalArgumentException> {
            GrainConfig(tenantAlias = "t", maxBatchSize = 200)
        }
    }

    @Test
    fun customConfig() {
        val config = GrainConfig(
            tenantAlias = "my-app",
            secret = "my-secret",
            debug = true,
            flushIntervalMs = 10_000L,
            flushThreshold = 10,
            maxBatchSize = 50,
        )
        assertEquals("my-app", config.tenantAlias)
        assertEquals("my-secret", config.secret)
        assertEquals(true, config.debug)
        assertEquals(10_000L, config.flushIntervalMs)
        assertEquals(10, config.flushThreshold)
        assertEquals(50, config.maxBatchSize)
    }
}
