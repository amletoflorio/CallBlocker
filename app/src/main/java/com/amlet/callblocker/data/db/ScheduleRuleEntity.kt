package com.amlet.callblocker.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single scheduling rule.
 *
 * mode:
 *   "activate"   — the blocker turns ON at [startTime] and OFF at [endTime] on the given [days].
 *   "deactivate" — the blocker turns OFF at [startTime] and ON at [endTime] on the given [days].
 *
 * days: comma-separated integers 1..7 (1=Mon … 7=Sun), e.g. "1,2,3,4,5".
 * startTime / endTime: "HH:mm" 24h strings.
 *
 * simTarget: which SIM(s) this rule applies to — "sim1", "sim2", or "both" (default).
 */
@Entity(tableName = "schedule_rules")
data class ScheduleRuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    /** "activate" or "deactivate" */
    val mode: String,

    /** Comma-separated day numbers, e.g. "1,2,3,4,5" */
    val days: String,

    /** HH:mm, e.g. "22:00" */
    val startTime: String,

    /** HH:mm, e.g. "07:00" */
    val endTime: String,

    /** Whether this rule is currently enabled. */
    val enabled: Boolean = true,

    /** "sim1", "sim2", or "both" */
    val simTarget: String = "both",

    /**
     * Determines which contacts this rule applies to.
     *
     * - [TARGET_ALL]      – the rule affects the global protection toggle (default behaviour).
     * - [TARGET_CATEGORY] – the rule activates/deactivates protection only for a specific
     *                       category; [targetId] holds the [CategoryEntity.id].
     * - [TARGET_CONTACT]  – the rule activates/deactivates protection only for a single
     *                       contact; [targetId] holds the [ContactEntity.id].
     */
    val targetType: String = TARGET_ALL,

    /**
     * The ID of the targeted category or contact when [targetType] is not [TARGET_ALL].
     * Null when [targetType] is [TARGET_ALL].
     */
    val targetId: Int? = null,

    /**
     * Optional user-defined name for this rule (e.g. "Night mode", "Weekend").
     * Null or blank means no custom label is shown.
     */
    val name: String? = null
) {
    /** Returns [days] as a sorted list of ints (1–7). */
    fun dayList(): List<Int> =
        days.split(",").mapNotNull { it.trim().toIntOrNull() }.sorted()

    companion object {
        const val MODE_ACTIVATE   = "activate"
        const val MODE_DEACTIVATE = "deactivate"

        /** Rule applies to the global protection state (default). */
        const val TARGET_ALL      = "all"

        /** Rule applies only to contacts belonging to a specific category. */
        const val TARGET_CATEGORY = "category"

        /** Rule applies only to a single whitelist contact. */
        const val TARGET_CONTACT  = "contact"
    }
}
