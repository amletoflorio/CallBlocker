package com.amlet.callblocker.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Represents a contact in the local whitelist.
 *
 * @Serializable → enables JSON serialisation for backup/restore
 * @Entity       → maps this data class to a SQLite table via Room
 */
@Serializable
@Entity(tableName = "allowed_contacts")
data class ContactEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    /** Normalised phone number, e.g. "391234567890" (no +, spaces, or dashes). */
    val phoneNumber: String,

    val name: String,

    /** Optional notes, e.g. "DHL courier", "Dentist". */
    val notes: String = "",

    /** Unix timestamp in milliseconds when this entry was added. */
    val addedAt: Long = System.currentTimeMillis(),

    /**
     * If non-null, this is a temporary entry that expires at the given timestamp (ms).
     * Once expired the number will be blocked again.
     * Shown with a visual badge in the whitelist UI.
     */
    val expiresAt: Long? = null,

    /**
     * Optional foreign key referencing [CategoryEntity.id].
     * Null means the contact has no category assigned.
     * The reference is stored as a plain Int (no Room FK constraint) so that
     * deleting a category does not cascade-delete its contacts; instead the
     * UI resets this field to null when a category is removed.
     */
    val categoryId: Int? = null
) {
    /** True if this is a temporary entry that has not yet expired. */
    val isTemporary: Boolean
        get() = expiresAt != null

    /** True if this temporary entry has already expired and should be removed. */
    val isExpired: Boolean
        get() = expiresAt != null && expiresAt < System.currentTimeMillis()

    /** Milliseconds remaining until expiry; 0 if already expired or permanent. */
    val remainingMs: Long
        get() = if (expiresAt == null) 0L else maxOf(0L, expiresAt - System.currentTimeMillis())
}
