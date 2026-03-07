@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.grainql.sdk.platform

import platform.Foundation.*

actual fun generateUUID(): String = NSUUID().UUIDString()

actual fun currentTimeMillis(): Long =
    (NSDate().timeIntervalSince1970 * 1000).toLong()

actual fun platformName(): String = "ios"

class IosKeyValueStore : KeyValueStore {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun getString(key: String): String? = defaults.stringForKey(key)

    override fun putString(key: String, value: String) {
        defaults.setObject(value, forKey = key)
    }

    override fun remove(key: String) {
        defaults.removeObjectForKey(key)
    }
}

class IosFileStore : FileStore {
    private val baseDir: String by lazy {
        val paths = NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory, NSUserDomainMask, true
        )
        (paths.firstOrNull() as? String) ?: NSTemporaryDirectory()
    }

    private fun path(filename: String) = "$baseDir/$filename"

    override fun read(filename: String): String? {
        return NSString.stringWithContentsOfFile(
            path(filename),
            encoding = NSUTF8StringEncoding,
            error = null,
        ) as? String
    }

    override fun write(filename: String, content: String) {
        (content as NSString).writeToFile(
            path(filename),
            atomically = true,
            encoding = NSUTF8StringEncoding,
            error = null,
        )
    }

    override fun delete(filename: String) {
        NSFileManager.defaultManager.removeItemAtPath(path(filename), error = null)
    }

    override fun exists(filename: String): Boolean {
        return NSFileManager.defaultManager.fileExistsAtPath(path(filename))
    }
}
