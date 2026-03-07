package com.grainql.sdk.platform

import java.io.File
import java.util.UUID
import java.util.prefs.Preferences

actual fun generateUUID(): String = UUID.randomUUID().toString()

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun platformName(): String = "jvm"

class JvmKeyValueStore : KeyValueStore {
    private val prefs = Preferences.userNodeForPackage(JvmKeyValueStore::class.java)

    override fun getString(key: String): String? = prefs.get(key, null)

    override fun putString(key: String, value: String) {
        prefs.put(key, value)
        prefs.flush()
    }

    override fun remove(key: String) {
        prefs.remove(key)
        prefs.flush()
    }
}

class JvmFileStore(baseDir: String? = null) : FileStore {
    private val dir = File(baseDir ?: "${System.getProperty("user.home")}/.grain-sdk")

    init {
        dir.mkdirs()
    }

    private fun file(filename: String) = File(dir, filename)

    override fun read(filename: String): String? {
        val f = file(filename)
        return if (f.exists()) f.readText() else null
    }

    override fun write(filename: String, content: String) {
        file(filename).writeText(content)
    }

    override fun delete(filename: String) {
        file(filename).delete()
    }

    override fun exists(filename: String): Boolean = file(filename).exists()
}
