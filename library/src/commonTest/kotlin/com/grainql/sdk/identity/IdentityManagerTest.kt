package com.grainql.sdk.identity

import com.grainql.sdk.platform.KeyValueStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class IdentityManagerTest {

    private class InMemoryKVStore : KeyValueStore {
        private val map = mutableMapOf<String, String>()
        override fun getString(key: String): String? = map[key]
        override fun putString(key: String, value: String) { map[key] = value }
        override fun remove(key: String) { map.remove(key) }
    }

    @Test
    fun generatesDeviceIdOnFirstUse() {
        val store = InMemoryKVStore()
        val mgr = IdentityManager(store)

        assertTrue(mgr.deviceId.isNotBlank())
        assertEquals(mgr.deviceId, mgr.getUserId())
        assertFalse(mgr.isIdentified)
    }

    @Test
    fun restoresDeviceIdFromStore() {
        val store = InMemoryKVStore()
        val mgr1 = IdentityManager(store)
        val deviceId = mgr1.deviceId

        val mgr2 = IdentityManager(store)
        assertEquals(deviceId, mgr2.deviceId)
    }

    @Test
    fun identifySetsExplicitUser() {
        val store = InMemoryKVStore()
        val mgr = IdentityManager(store)

        mgr.identify("user-123")
        assertEquals("user-123", mgr.getUserId())
        assertTrue(mgr.isIdentified)
    }

    @Test
    fun resetClearsIdentityButKeepsDeviceId() {
        val store = InMemoryKVStore()
        val mgr = IdentityManager(store)
        val deviceId = mgr.deviceId

        mgr.identify("user-123")
        mgr.reset()

        assertFalse(mgr.isIdentified)
        assertEquals(deviceId, mgr.deviceId)
        assertEquals(deviceId, mgr.getUserId())
    }

    @Test
    fun deviceIdStableAcrossResets() {
        val store = InMemoryKVStore()
        val mgr = IdentityManager(store)
        val deviceId = mgr.deviceId

        mgr.identify("alice")
        mgr.reset()
        mgr.identify("bob")
        mgr.reset()

        assertEquals(deviceId, mgr.getUserId())
    }

    @Test
    fun sessionIdIsStable() {
        val store = InMemoryKVStore()
        val mgr = IdentityManager(store)

        assertEquals(mgr.sessionId, mgr.sessionId)
    }

    @Test
    fun sessionIdDiffsAcrossInstances() {
        val store = InMemoryKVStore()
        val mgr1 = IdentityManager(store)
        val mgr2 = IdentityManager(store)

        assertNotEquals(mgr1.sessionId, mgr2.sessionId)
    }
}
