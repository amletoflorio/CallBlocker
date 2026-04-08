package com.amlet.callblocker.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Represents a user-defined category that can be assigned to whitelist contacts.
 *
 * Categories allow grouping contacts (e.g. "Family", "Work", "Couriers") and
 * can be used as a target in scheduling rules to activate or deactivate protection
 * only for contacts belonging to a specific category.
 *
 * @Serializable enables JSON serialisation for backup/restore.
 * @Entity       maps this class to a SQLite table via Room.
 */
@Serializable
@Entity(tableName = "contact_categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    /** Display name shown in the UI, e.g. "Family", "Work". */
    val name: String,

    /**
     * Optional emoji or short symbol used as a visual icon in the UI.
     * If blank, a default icon is shown instead.
     */
    val emoji: String = "",

    /** Unix timestamp (ms) when this category was created. */
    val createdAt: Long = System.currentTimeMillis()
)
