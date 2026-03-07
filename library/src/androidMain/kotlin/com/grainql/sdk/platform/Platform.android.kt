package com.grainql.sdk.platform

import android.content.Context
import android.content.SharedPreferences
import java.io.File
import java.util.UUID

actual fun generateUUID(): String = UUID.randomUUID().toString()

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun platformName(): String = "android"

class AndroidKeyValueStore(context: Context) : KeyValueStore {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("grain_sdk", Context.MODE_PRIVATE)

    override fun getString(key: String): String? = prefs.getString(key, null)

    override fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    override fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }
}

class AndroidFileStore(private val context: Context) : FileStore {
    private fun file(filename: String) = File(context.filesDir, filename)

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
