package com.grainql.sdk

import com.grainql.sdk.core.EventDispatcher
import com.grainql.sdk.core.EventQueue
import com.grainql.sdk.core.EventTransport
import com.grainql.sdk.identity.IdentityManager
import com.grainql.sdk.model.GrainEvent
import com.grainql.sdk.persistence.EventStore
import com.grainql.sdk.platform.*
import co.touchlab.kermit.Logger
import kotlinx.coroutines.*
import kotlinx.serialization.json.*

object GrainAnalytics {

    private val log = Logger.withTag("Grain")

    private var config: GrainConfig? = null
    private var identityManager: IdentityManager? = null
    private var dispatcher: EventDispatcher? = null
    private var transport: EventTransport? = null
    private var scope: CoroutineScope? = null
    private var initialized = false

    const val SDK_VERSION = "1.0.0"

    fun initialize(
        config: GrainConfig,
        kvStore: KeyValueStore,
        fileStore: FileStore? = null,
    ) {
        if (initialized) {
            log.w { "Grain SDK already initialized. Call shutdown() first to reinitialize." }
            return
        }

        this.config = config
        this.scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        val identity = IdentityManager(kvStore)
        this.identityManager = identity

        val queue = EventQueue(config.maxQueueSize)
        val eventTransport = EventTransport(config)
        this.transport = eventTransport

        val store = if (config.enablePersistence && fileStore != null) {
            EventStore(fileStore)
        } else null

        val eventDispatcher = EventDispatcher(config, queue, eventTransport, store, scope!!)
        this.dispatcher = eventDispatcher

        initialized = true

        scope!!.launch {
            eventDispatcher.restorePersistedEvents()
            eventDispatcher.start()
        }

        if (config.debug) {
            log.d { "Grain SDK initialized for tenant '${config.tenantAlias}'" }
            log.d { "Device ID: ${identity.deviceId}" }
            log.d { "Session ID: ${identity.sessionId}" }
        }
    }

    fun track(eventName: String, properties: Map<String, Any> = emptyMap()) {
        val mgr = requireInitialized() ?: return
        trackInternal(eventName, mgr.getUserId(), properties)
    }

    fun track(eventName: String, userId: String, properties: Map<String, Any> = emptyMap()) {
        requireInitialized() ?: return
        trackInternal(eventName, userId, properties)
    }

    fun identify(userId: String) {
        val mgr = requireInitialized() ?: return
        mgr.identify(userId)
        if (config?.debug == true) log.d { "Identified user: $userId" }
    }

    fun resetIdentity() {
        val mgr = requireInitialized() ?: return
        mgr.reset()
        if (config?.debug == true) log.d { "Identity reset. userId reverted to device ID: ${mgr.deviceId}" }
    }

    fun getDeviceId(): String? {
        return identityManager?.deviceId
    }

    fun getSessionId(): String? {
        return identityManager?.sessionId
    }

    fun setUserProperties(properties: Map<String, String>) {
        val mgr = requireInitialized() ?: return
        setUserProperties(mgr.getUserId(), properties)
    }

    fun setUserProperties(userId: String, properties: Map<String, String>) {
        requireInitialized() ?: return
        scope?.launch {
            transport?.sendUserProperties(userId, properties)
        }
    }

    suspend fun flush() {
        dispatcher?.flush()
    }

    fun onForeground() {
        dispatcher?.resume()
    }

    fun onBackground() {
        val d = dispatcher ?: return
        scope?.launch {
            d.flush()
            d.pause()
        }
    }

    fun onNetworkConnected() {
        dispatcher?.setNetworkAvailable(true)
    }

    fun onNetworkDisconnected() {
        dispatcher?.setNetworkAvailable(false)
    }

    suspend fun shutdown() {
        if (!initialized) return

        val d = dispatcher
        val s = scope

        // Mark as not initialized first to reject new events
        initialized = false
        config = null
        identityManager = null
        dispatcher = null
        transport = null
        this.scope = null

        // Flush remaining events (drains pending channel too), then cancel the scope
        d?.shutdown()
        s?.cancel()

        log.d { "Grain SDK shut down" }
    }

    val isInitialized: Boolean get() = initialized

    private fun trackInternal(eventName: String, userId: String, properties: Map<String, Any>) {
        val identity = identityManager ?: return
        val cfg = config ?: return
        val d = dispatcher ?: return

        val jsonProps = properties.mapValues { (_, v) -> v.toJsonElement() }

        val systemProps = buildMap<String, JsonElement> {
            put("session_id", JsonPrimitive(identity.sessionId))
            put("device_id", JsonPrimitive(identity.deviceId))
            put("client_version", JsonPrimitive(SDK_VERSION))
            put("platform", JsonPrimitive(platformName()))
            put("timestamp", JsonPrimitive(currentTimeMillis()))
        }

        val event = GrainEvent(
            id = generateUUID(),
            eventName = eventName,
            userId = userId,
            properties = jsonProps,
            systemProperties = systemProps,
            timestamp = currentTimeMillis(),
        )

        // Synchronous submit into unbounded channel — no coroutine launch needed.
        // flush()/shutdown() drain this channel first, so no event is lost.
        d.submit(event)
        d.checkThreshold()

        if (cfg.debug) log.d { "Tracked: $eventName (user=$userId, props=${properties.size})" }
    }

    private fun requireInitialized(): IdentityManager? {
        if (!initialized || identityManager == null) {
            log.e { "Grain SDK not initialized. Call GrainAnalytics.initialize() first." }
            return null
        }
        return identityManager
    }
}

internal fun Any.toJsonElement(): JsonElement = when (this) {
    is Number -> JsonPrimitive(this)
    is Boolean -> JsonPrimitive(this)
    is String -> JsonPrimitive(this)
    is Map<*, *> -> JsonObject(
        entries.associate { (k, v) -> k.toString() to (v?.toJsonElement() ?: JsonNull) }
    )
    is Collection<*> -> JsonArray(map { it?.toJsonElement() ?: JsonNull })
    is JsonElement -> this
    else -> JsonPrimitive(toString())
}
