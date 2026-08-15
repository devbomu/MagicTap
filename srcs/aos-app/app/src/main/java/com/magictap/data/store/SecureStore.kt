package com.magictap.data.store

import android.content.Context
import android.util.Log
import com.magictap.data.crypto.KeystoreManager
import com.magictap.data.model.AppData
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Persists the single [AppData] document as one Keystore-encrypted file in internal
 * storage. Reads and writes are blocking crypto/file-IO — call from a background
 * dispatcher (the repository does this).
 */
class SecureStore(context: Context) {

    private val file = File(context.applicationContext.filesDir, FILE_NAME)
    private val lock = Any()

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun load(): AppData = synchronized(lock) {
        if (!file.exists()) return AppData()
        return try {
            val plain = KeystoreManager.decrypt(file.readBytes())
            json.decodeFromString(AppData.serializer(), plain.decodeToString())
        } catch (e: Exception) {
            // Corrupt file or an invalidated key. Rather than crash on every launch,
            // start from empty; the user can re-import a backup.
            Log.e(TAG, "Failed to read store; starting empty", e)
            AppData()
        }
    }

    fun save(data: AppData): Unit = synchronized(lock) {
        val plain = json.encodeToString(AppData.serializer(), data).encodeToByteArray()
        val encrypted = KeystoreManager.encrypt(plain)
        // Write to a temp file then rename, so a crash mid-write can't truncate the store.
        val tmp = File(file.parentFile, "$FILE_NAME.tmp")
        tmp.writeBytes(encrypted)
        if (!tmp.renameTo(file)) {
            file.writeBytes(encrypted)
            tmp.delete()
        }
    }

    private companion object {
        const val FILE_NAME = "magictap.store"
        const val TAG = "SecureStore"
    }
}
