package com.amlet.callblocker.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Rappresenta un contatto nella whitelist locale.
 *
 * @Serializable → permette la serializzazione JSON per il backup
 * @Entity       → mappa questa data class a una tabella SQLite
 */
@Serializable
@Entity(tableName = "allowed_contacts")
data class ContactEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    /** Numero normalizzato, es. "391234567890" (senza +, spazi, trattini) */
    val phoneNumber: String,

    val name: String,

    /** Note opzionali, es. "Corriere DHL", "Dentista" */
    val notes: String = "",

    /** Timestamp Unix in millisecondi */
    val addedAt: Long = System.currentTimeMillis()
)