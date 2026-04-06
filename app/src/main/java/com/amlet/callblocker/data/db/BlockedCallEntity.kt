package com.amlet.callblocker.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a single blocked (or monitored) call in the log.
 * Each row is one event; aggregation is performed at query level via [BlockedCallDao].
 */
@Entity(tableName = "blocked_calls")
data class BlockedCallEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    /** Normalised number (digits only, no leading zeros) — see PhoneUtils.normalize(). */
    val phoneNumber: String,

    /** Unix timestamp in milliseconds of this specific event. */
    val blockedAt: Long = System.currentTimeMillis(),

    /**
     * SIM slot identifier, if determinable (null = unknown / single-SIM).
     * Values: "SIM1", "SIM2", or null.
     */
    val simSlot: String? = null,

    /**
     * Call direction as returned by Call.Details.getCallDirection() (API 29+).
     * Possible values: "incoming", "unknown", or null on older APIs.
     * Outgoing calls must NEVER be logged — the service filters them before insert.
     */
    val callDirection: String? = null,

    /**
     * Raw accountHandle ID string from the telecom subsystem.
     * Useful for debugging SIM detection on unusual OEM firmware.
     */
    val accountHandleId: String? = null,

    /**
     * "incoming" — kept for historical compatibility; all entries are calls.
     */
    val callType: String = TYPE_CALL,

    /**
     * STIR/SHAKEN caller number verification status (API 31+).
     * Possible values: "passed", "failed", "not_verified", or null (API < 31).
     */
    val verificationStatus: String? = null,

    /**
     * When non-null, the call was *allowed* (not blocked) for this reason.
     * Values: "outgoing_recent" (dialed whitelist), "retry_rule".
     * Null means the call was blocked normally.
     */
    val allowReason: String? = null,
) {
    companion object {
        const val TYPE_CALL = "incoming"

        // verificationStatus constants
        const val VERIF_PASSED       = "passed"
        const val VERIF_FAILED       = "failed"
        const val VERIF_NOT_VERIFIED = "not_verified"

        // allowReason constants
        const val ALLOW_OUTGOING_RECENT = "outgoing_recent"
        const val ALLOW_RETRY_RULE      = "retry_rule"
    }
}
