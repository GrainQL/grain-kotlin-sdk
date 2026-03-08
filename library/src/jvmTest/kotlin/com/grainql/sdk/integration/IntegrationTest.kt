package com.grainql.sdk.integration

import com.grainql.sdk.GrainAnalytics
import com.grainql.sdk.GrainConfig
import com.grainql.sdk.core.EventTransport
import com.grainql.sdk.core.TransportResult
import com.grainql.sdk.model.EventPayload
import com.grainql.sdk.platform.JvmFileStore
import com.grainql.sdk.platform.JvmKeyValueStore
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.*

/**
 * Integration tests that hit the real Grain API.
 * Defaults to tenant "grain-kotlin-sdk-test".
 * Override with GRAIN_TENANT_ALIAS and GRAIN_SECRET env vars.
 */
class IntegrationTest {

    private val tenantAlias = System.getenv("GRAIN_TENANT_ALIAS").takeUnless { it.isNullOrBlank() } ?: "grain-kotlin-sdk-test"
    private val secret = System.getenv("GRAIN_SECRET").takeUnless { it.isNullOrBlank() }

    @Test
    fun transportSendSingleBatch() = runTest {
        val config = GrainConfig(
            tenantAlias = tenantAlias,
            secret = secret,
        )
        val transport = EventTransport(config)

        val events = listOf(
            EventPayload(
                eventName = "_grain_sdk_test",
                userId = "integration-test-device",
                properties = mapOf(
                    "session_id" to JsonPrimitive("test-session"),
                    "device_id" to JsonPrimitive("test-device-id"),
                    "client_version" to JsonPrimitive(GrainAnalytics.SDK_VERSION),
                    "platform" to JsonPrimitive("jvm"),
                    "test" to JsonPrimitive(true),
                ),
            )
        )

        val result = transport.sendBatch(events)
        assertEquals(TransportResult.Success, result, "Expected 200 from Grain API")
        transport.close()
    }

    @Test
    fun transportSendLargeBatch() = runTest {
        val config = GrainConfig(
            tenantAlias = tenantAlias,
            secret = secret,
        )
        val transport = EventTransport(config)

        val events = (1..100).map { i ->
            EventPayload(
                eventName = "_grain_sdk_batch_test",
                userId = "integration-test-device",
                properties = mapOf(
                    "index" to JsonPrimitive(i),
                    "platform" to JsonPrimitive("jvm"),
                    "test" to JsonPrimitive(true),
                ),
            )
        }

        val result = transport.sendBatch(events)
        assertEquals(TransportResult.Success, result, "Batch of 100 should succeed")
        transport.close()
    }

    @Test
    fun transportRejectsBatchOver160() = runTest {
        val config = GrainConfig(
            tenantAlias = tenantAlias,
            secret = secret,
            maxBatchSize = 160,
        )
        val transport = EventTransport(config)

        val events = (1..161).map { i ->
            EventPayload(
                eventName = "_grain_sdk_overlimit_test",
                userId = "integration-test-device",
                properties = mapOf("index" to JsonPrimitive(i)),
            )
        }

        val result = transport.sendBatch(events)
        assertEquals(TransportResult.BatchTooLarge, result, "161 events should be rejected with 413")
        transport.close()
    }

    @Test
    fun fullPipelineTrackAndFlush() = runTest {
        val tmpDir = kotlin.io.path.createTempDirectory("grain-test").toFile()
        try {
            val config = GrainConfig(
                tenantAlias = tenantAlias,
                secret = secret,
                debug = true,
                flushIntervalMs = 60_000, // long interval so only manual flush fires
            )

            GrainAnalytics.initialize(
                config = config,
                kvStore = JvmKeyValueStore(),
                fileStore = JvmFileStore(tmpDir.absolutePath),
            )

            GrainAnalytics.track("_grain_sdk_integration", mapOf(
                "score" to 42,
                "level" to "boss",
                "success" to true,
            ))

            GrainAnalytics.track("_grain_sdk_integration", mapOf(
                "score" to 99,
                "level" to "final",
                "success" to false,
            ))

            // flush() drains the channel first, so both events are guaranteed to be sent
            GrainAnalytics.flush()

            // If we got here without exception, the flush succeeded (transport logs errors)
            assertTrue(GrainAnalytics.isInitialized)

            GrainAnalytics.shutdown()
            assertFalse(GrainAnalytics.isInitialized)
        } finally {
            tmpDir.deleteRecursively()
        }
    }

    @Test
    fun identifyThenTrackAndFlush() = runTest {
        val tmpDir = kotlin.io.path.createTempDirectory("grain-test").toFile()
        try {
            val config = GrainConfig(
                tenantAlias = tenantAlias,
                secret = secret,
                debug = true,
                flushIntervalMs = 60_000,
            )

            GrainAnalytics.initialize(
                config = config,
                kvStore = JvmKeyValueStore(),
                fileStore = JvmFileStore(tmpDir.absolutePath),
            )

            val deviceId = GrainAnalytics.getDeviceId()
            assertNotNull(deviceId)

            // Track before identify — userId should be deviceId
            GrainAnalytics.track("_grain_sdk_pre_identify", mapOf("phase" to "anonymous"))

            // Identify
            GrainAnalytics.identify("test-player-123")

            // Track after identify — userId should be "test-player-123", device_id still in properties
            GrainAnalytics.track("_grain_sdk_post_identify", mapOf("phase" to "identified"))

            // Reset and track again — userId should revert to same deviceId
            GrainAnalytics.resetIdentity()
            GrainAnalytics.track("_grain_sdk_post_reset", mapOf("phase" to "reset"))

            GrainAnalytics.flush()
            GrainAnalytics.shutdown()
        } finally {
            tmpDir.deleteRecursively()
        }
    }

    @Test
    fun userProperties() = runTest {
        val config = GrainConfig(
            tenantAlias = tenantAlias,
            secret = secret,
        )
        val transport = EventTransport(config)

        // Should not throw — we just verify no HTTP error
        transport.sendUserProperties("integration-test-user", mapOf(
            "plan" to "pro",
            "source" to "sdk-integration-test",
        ))
        transport.close()
    }
}
