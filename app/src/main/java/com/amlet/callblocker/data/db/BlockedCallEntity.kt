package com.amlet.callblocker.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a single blocked call attempt in the log.
 * Each row is one call event; aggregation (total attempts, per-number grouping)
 * is performed at query level via [BlockedCallDao].
 */
@Entity(tableName = "blocked_calls")
data class BlockedCallEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    /** Normalised number (digits only, no leading zeros) — see PhoneUtils.normalize(). */
    val phoneNumber: String,

    /** Unix timestamp in milliseconds of this specific call attempt. */
    val blockedAt: Long = System.currentTimeMillis(),

    /**
     * SIM slot identifier, if determinable (null = unknown / single-SIM).
     * Values: "SIM1", "SIM2", or null.
     */
    val simSlot: String? = null,

    /**
     * Call direction as returned by Call.Details.getCallDirection() (API 29+).
     * Possible values: "Incoming", "Unknown", or null on older APIs.
     */
    val callDirection: String? = null,

    /**
     * Raw accountHandle ID string from the telecom subsystem.
     * Useful for debugging SIM detection on unusual OEM firmware.
     */
    val accountHandleId: String? = null
)
