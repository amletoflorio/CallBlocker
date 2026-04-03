package com.amlet.callblocker.data.backup

import android.content.Context
import android.net.Uri
import com.amlet.callblocker.data.db.ContactEntity
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * Handles import and export of the whitelist in JSON format.
 *
 * Uses Android's SAF (Storage Access Framework) to access files
 * safely and compatibly across modern Android versions.
 * The Uri is provided by the user via the system file picker.
 */
object BackupManager {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true // Forward compatibility with future versions
    }

    @kotlinx.serialization.Serializable
    data class BackupFile(
        val version: Int = 1,
        val exportedAt: Long = System.currentTimeMillis(),
        val contacts: List<ContactEntity>
    )

    /** Exports contacts to a JSON file chosen by the user (via SAF). */
    fun exportToUri(
        context: Context,
        uri: Uri,
        contacts: List<ContactEntity>
    ): Result<Unit> {
        return try {
            val backup = BackupFile(contacts = contacts)
            val jsonString = json.encodeToString(backup)

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(jsonString.toByteArray(Charsets.UTF_8))
            } ?: return Result.failure(Exception("Cannot open destination file"))

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Exports contacts directly to a [File] on the filesystem.
     * Used by [com.amlet.callblocker.worker.AutoBackupWorker] for automatic backups.
     */
    fun exportToFile(
        context: Context,
        file: File,
        contacts: List<ContactEntity>
    ): Result<Unit> {
        return try {
            val backup = BackupFile(contacts = contacts)
            val jsonString = json.encodeToString(backup)
            file.writeText(jsonString, Charsets.UTF_8)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Imports contacts from a JSON file chosen by the user. */
    fun importFromUri(
        context: Context,
        uri: Uri
    ): Result<List<ContactEntity>> {
        return try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).readText()
            } ?: return Result.failure(Exception("Cannot read file"))

            // Try the BackupFile format first (v1+)
            val contacts = try {
                val backup = json.decodeFromString<BackupFile>(jsonString)
                backup.contacts
            } catch (e: Exception) {
                // Fallback: direct list of ContactEntity (legacy format)
                json.decodeFromString<List<ContactEntity>>(jsonString)
            }

            // Reset IDs to avoid conflicts with the existing DB
            val resetContacts = contacts.map { it.copy(id = 0) }
            Result.success(resetContacts)

        } catch (e: Exception) {
            Result.failure(Exception("Invalid or corrupted file: ${e.message}"))
        }
    }
}
