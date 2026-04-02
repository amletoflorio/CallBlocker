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
 * Gestisce l'import e l'export della whitelist in formato JSON.
 *
 * Usa il SAF (Storage Access Framework) di Android per accedere ai file
 * in modo sicuro e compatibile con tutte le versioni moderne.
 * L'Uri viene fornito dall'utente tramite il file picker di sistema.
 */
object BackupManager {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true // Compatibilità forward con versioni future
    }

    @kotlinx.serialization.Serializable
    data class BackupFile(
        val version: Int = 1,
        val exportedAt: Long = System.currentTimeMillis(),
        val contacts: List<ContactEntity>
    )

    /**
     * Esporta i contatti in un file JSON scelto dall'utente (via SAF).
     */
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
            } ?: return Result.failure(Exception("Impossibile aprire il file di destinazione"))

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Esporta i contatti direttamente in un [File] del filesystem.
     * Usato da [com.amlet.callblocker.worker.AutoBackupWorker] per il backup automatico.
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

    /**
     * Importa i contatti da un file JSON scelto dall'utente.
     */
    fun importFromUri(
        context: Context,
        uri: Uri
    ): Result<List<ContactEntity>> {
        return try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).readText()
            } ?: return Result.failure(Exception("Impossibile leggere il file"))

            // Proviamo prima il formato BackupFile (v1+)
            val contacts = try {
                val backup = json.decodeFromString<BackupFile>(jsonString)
                backup.contacts
            } catch (e: Exception) {
                // Fallback: lista diretta di ContactEntity (formato legacy)
                json.decodeFromString<List<ContactEntity>>(jsonString)
            }

            // Resetta gli ID per evitare conflitti con il DB esistente
            val resetContacts = contacts.map { it.copy(id = 0) }
            Result.success(resetContacts)

        } catch (e: Exception) {
            Result.failure(Exception("File non valido o corrotto: ${e.message}"))
        }
    }
}
