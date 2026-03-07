<br>
<p align="center">
  <img src="https://cdn.xreos.co/webasset/images/grain_logomark_dark.svg" alt="Grain" width="150"/>
</p>

<h1 align="center">Grain Kotlin SDK</h1>

<p align="center">
  Kotlin Multiplatform analytics client for <a href="https://grainql.com">Grain</a>.<br/>
  Works on Android, iOS, and JVM.
</p>

<p align="center">
  <a href="https://central.sonatype.com/artifact/com.grainql/grain-sdk"><img src="https://img.shields.io/maven-central/v/com.grainql/grain-sdk" alt="Maven Central" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-MIT-blue.svg" alt="License" /></a>
</p>

---

## Installation

### Gradle (Kotlin DSL)

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.grainql:grain-sdk:0.0.1")
}
```

### Version Catalog

```toml
[libraries]
grain-sdk = { module = "com.grainql:grain-sdk", version = "0.0.1" }
```

## Quick Start

### Android

```kotlin
import com.grainql.sdk.GrainAnalytics
import com.grainql.sdk.GrainConfig
import com.grainql.sdk.platform.AndroidKeyValueStore
import com.grainql.sdk.platform.AndroidFileStore

// Initialize (typically in Application.onCreate)
GrainAnalytics.initialize(
    config = GrainConfig(tenantAlias = "your-tenant"),
    kvStore = AndroidKeyValueStore(applicationContext),
    fileStore = AndroidFileStore(applicationContext),
)

// Track events
GrainAnalytics.track("button_clicked", mapOf(
    "screen" to "home",
    "button" to "signup",
))

// Identify a user after login
GrainAnalytics.identify("user-123")

// Track with typed properties — numbers and booleans stay typed over the wire
GrainAnalytics.track("purchase_complete", mapOf(
    "amount" to 29.99,
    "currency" to "USD",
    "first_purchase" to true,
))
```

### iOS (from Swift via KMP framework)

```swift
let kvStore = IosKeyValueStore()
let fileStore = IosFileStore()

GrainAnalytics.shared.initialize(
    config: GrainConfig(tenantAlias: "your-tenant"),
    kvStore: kvStore,
    fileStore: fileStore
)

GrainAnalytics.shared.track(eventName: "screen_viewed", properties: ["screen": "settings"])
```

### JVM

```kotlin
import com.grainql.sdk.platform.JvmKeyValueStore
import com.grainql.sdk.platform.JvmFileStore

GrainAnalytics.initialize(
    config = GrainConfig(tenantAlias = "your-tenant"),
    kvStore = JvmKeyValueStore(),
    fileStore = JvmFileStore(),
)
```

## Configuration

```kotlin
GrainConfig(
    tenantAlias = "your-tenant",           // Required
    apiUrl = "https://clientapis.grainql.com",  // Default
    secret = null,                         // Tenant secret for authenticated ingestion
    debug = false,                         // Enable debug logging
    flushIntervalMs = 30_000,              // Periodic flush interval (30s default)
    flushThreshold = 25,                   // Flush when queue reaches this size
    maxBatchSize = 100,                    // Max events per API call (server limit: 160)
    maxQueueSize = 10_000,                 // Max events in local queue
    maxRetries = 5,                        // Retry attempts per batch on failure
    enablePersistence = true,              // Persist events to disk for offline support
)
```

## API Reference

### Event Tracking

```kotlin
GrainAnalytics.track(eventName, properties)           // Track with current user
GrainAnalytics.track(eventName, userId, properties)    // Track with explicit user
```

### Identity

```kotlin
GrainAnalytics.identify(userId)      // Set the user identity
GrainAnalytics.resetIdentity()       // Clear identity, revert to device ID
GrainAnalytics.getDeviceId()         // Stable device ID (persists across resets)
GrainAnalytics.getSessionId()        // Current session ID (fresh per app launch)
```

### User Properties

```kotlin
GrainAnalytics.setUserProperties(mapOf("plan" to "pro", "source" to "organic"))
GrainAnalytics.setUserProperties(userId, properties)   // For a specific user
```

### Lifecycle

```kotlin
GrainAnalytics.onForeground()         // Resume periodic flushing
GrainAnalytics.onBackground()         // Flush pending events, pause timer
GrainAnalytics.onNetworkConnected()   // Flush immediately
GrainAnalytics.onNetworkDisconnected() // Pause sending
GrainAnalytics.flush()                // Manual flush (suspend)
GrainAnalytics.shutdown()             // Final flush and cleanup (suspend)
```

## How It Works

Events flow through a simple pipeline:

1. `track()` synchronously submits events into an unbounded channel — it never blocks.
2. Before every flush, pending events are drained into an in-memory queue.
3. The dispatcher batches events and POSTs them to the Grain API.
4. Failed batches are retried with exponential backoff. Events exceeding `maxRetries` are dropped.
5. When persistence is enabled, events are written to a local JSONL file and removed after successful delivery.

The SDK automatically attaches `session_id`, `device_id`, `client_version`, `platform`, and `timestamp` to every event. These match the conventions used by the [Grain web SDK](https://github.com/grainql/grain-tag) so all events participate in the same analytics pipeline.

## Building from Source

```bash
./gradlew :library:jvmTest                          # Run tests
./gradlew :library:compileAndroidMain               # Build Android
./gradlew :library:compileKotlinIosArm64            # Build iOS
```

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

## License

[MIT](LICENSE)
