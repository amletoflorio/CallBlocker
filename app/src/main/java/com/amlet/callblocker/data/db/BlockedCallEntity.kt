package com.amlet.callblocker.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Rappresenta una chiamata bloccata nel log.
 * Tabella separata da allowed_contacts — non mescolare le responsabilità.
 */
@Entity(tableName = "blocked_calls")
data class BlockedCallEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    /** Numero grezzo come arriva dal sistema, es. "+390694807697" */
    val phoneNumber: String,

    /** Timestamp Unix in millisecondi */
    val blockedAt: Long = System.currentTimeMillis()
)
