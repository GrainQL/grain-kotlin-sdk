package com.grainql.sdk.identity

import com.grainql.sdk.platform.KeyValueStore
import com.grainql.sdk.platform.generateUUID

internal class IdentityManager(private val kvStore: KeyValueStore) {

    private var explicitUserId: String? = null
    val sessionId: String = generateUUID()

    val deviceId: String = kvStore.getString(KEY_DEVICE_ID) ?: generateUUID().also {
        kvStore.putString(KEY_DEVICE_ID, it)
    }

    fun getUserId(): String = explicitUserId ?: deviceId

    fun identify(userId: String) {
        explicitUserId = userId
    }

    fun reset() {
        explicitUserId = null
    }

    val isIdentified: Boolean get() = explicitUserId != null

    companion object {
        private const val KEY_DEVICE_ID = "grain_device_id"
    }
}
