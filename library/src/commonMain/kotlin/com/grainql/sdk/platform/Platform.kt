package com.grainql.sdk.platform

expect fun generateUUID(): String

expect fun currentTimeMillis(): Long

expect fun platformName(): String

interface KeyValueStore {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
    fun remove(key: String)
}

interface FileStore {
    fun read(filename: String): String?
    fun write(filename: String, content: String)
    fun delete(filename: String)
    fun exists(filename: String): Boolean
}
