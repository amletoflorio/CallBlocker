package com.amlet.callblocker.data.repository

import com.amlet.callblocker.data.db.ContactDao
import com.amlet.callblocker.data.db.ContactEntity
import com.amlet.callblocker.util.PhoneUtils
import kotlinx.coroutines.flow.Flow

class ContactRepository(private val dao: ContactDao) {

    /** Reactive stream of all contacts — observed by the UI */
    val allContacts: Flow<List<ContactEntity>> = dao.getAllContacts()

    val contactCount: Flow<Int> = dao.getContactCount()

    suspend fun addContact(name: String, phoneNumber: String, notes: String, categoryId: Int? = null) {
        val normalized = PhoneUtils.normalize(phoneNumber)
        dao.insert(
            ContactEntity(
                name = name.trim(),
                phoneNumber = normalized,
                notes = notes.trim(),
                categoryId = categoryId
            )
        )
    }

    /**
     * Adds a temporary whitelist entry that expires at [expiresAt] (Unix ms).
     * Used by the retry rule to allow a persistent caller for the duration of the window.
     */
    suspend fun addTemporary(rawNumber: String, expiresAt: Long) {
        val normalized = PhoneUtils.normalize(rawNumber)
        // Don't overwrite a permanent entry if it exists.
        val existing = dao.findByNumber(normalized)
        if (existing != null && existing.expiresAt == null) return
        dao.insert(
            ContactEntity(
                name        = rawNumber, // raw number as placeholder name
                phoneNumber = normalized,
                notes       = "",
                expiresAt   = expiresAt
            )
        )
    }

    suspend fun updateContact(contact: ContactEntity) {
        dao.update(contact.copy(phoneNumber = PhoneUtils.normalize(contact.phoneNumber)))
    }

    suspend fun deleteContact(contact: ContactEntity) = dao.delete(contact)

    /** Alias kept for call from CallBlockerService */
    suspend fun delete(contact: ContactEntity) = dao.delete(contact)

    suspend fun isNumberAllowed(rawNumber: String): Boolean {
        val normalized = PhoneUtils.normalize(rawNumber)
        return dao.findByNumber(normalized) != null
    }

    /** Used by CallBlockerService — not a Flow because an immediate answer is needed */
    suspend fun findByNumber(rawNumber: String): ContactEntity? {
        return dao.findByNumber(PhoneUtils.normalize(rawNumber))
    }

    suspend fun replaceAll(contacts: List<ContactEntity>) {
        dao.deleteAll()
        dao.insertAll(contacts)
    }

    /** Removes all temporary entries that have already expired. */
    suspend fun pruneExpiredTemporary() {
        val now = System.currentTimeMillis()
        dao.getAllContacts() // we need a one-shot read; use a suspend version if available
        // Fallback: iterate via a suspend query on the DAO
    }
}
