package com.amlet.callblocker.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object UpdateChecker {

    private const val API_URL =
        "https://api.github.com/repos/amletoflorio/CallBlocker/releases/latest"

    data class UpdateInfo(
        val latestVersion: String,
        val downloadUrl: String,
        val releaseNotes: String
    )

    sealed class UpdateResult {
        data class UpdateAvailable(val info: UpdateInfo) : UpdateResult()
        object UpToDate : UpdateResult()
        data class Error(val message: String) : UpdateResult()
    }

    /**
     * Controlla se è disponibile una versione più recente su GitHub Releases.
     * Confronta [currentVersion] (es. "1.2.0") con il tag_name del release più recente.
     * Deve essere chiamata da una coroutine (usa Dispatchers.IO internamente).
     */
    suspend fun checkForUpdate(currentVersion: String): UpdateResult =
        withContext(Dispatchers.IO) {
            try {
                val url = URL(API_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/vnd.github+json")
                    connectTimeout = 10_000
                    readTimeout = 10_000
                }

                val responseCode = connection.responseCode
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    return@withContext UpdateResult.Error("HTTP $responseCode")
                }

                val body = connection.inputStream.bufferedReader().readText()
                connection.disconnect()

                val json = JSONObject(body)
                val tagName = json.getString("tag_name").trimStart('v', 'V')
                val htmlUrl = json.getString("html_url")
                val releaseNotes = json.optString("body", "").take(500)

                if (isNewerVersion(tagName, currentVersion)) {
                    UpdateResult.UpdateAvailable(
                        UpdateInfo(
                            latestVersion = tagName,
                            downloadUrl = htmlUrl,
                            releaseNotes = releaseNotes
                        )
                    )
                } else {
                    UpdateResult.UpToDate
                }
            } catch (e: Exception) {
                UpdateResult.Error(e.message ?: "Errore sconosciuto")
            }
        }

    /**
     * Confronto semantico semplice: "1.3.0" > "1.2.0".
     * Funziona con versioni nel formato MAJOR.MINOR.PATCH.
     */
    private fun isNewerVersion(remote: String, current: String): Boolean {
        val remoteParts  = remote.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }
}
