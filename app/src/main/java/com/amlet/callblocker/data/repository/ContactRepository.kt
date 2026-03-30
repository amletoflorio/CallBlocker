package com.amlet.callblocker.data.repository

import com.amlet.callblocker.data.db.ContactDao
import com.amlet.callblocker.data.db.ContactEntity
import com.amlet.callblocker.util.PhoneUtils
import kotlinx.coroutines.flow.Flow

class ContactRepository(private val dao: ContactDao) {

    /** Stream reattivo di tutti i contatti — la UI osserva questo */
    val allContacts: Flow<List<ContactEntity>> = dao.getAllContacts()

    val contactCount: Flow<Int> = dao.getContactCount()

    suspend fun addContact(name: String, phoneNumber: String, notes: String) {
        val normalized = PhoneUtils.normalize(phoneNumber)
        dao.insert(
            ContactEntity(
                name = name.trim(),
                phoneNumber = normalized,
                notes = notes.trim()
            )
        )
    }

    suspend fun updateContact(contact: ContactEntity) {
        dao.update(contact.copy(phoneNumber = PhoneUtils.normalize(contact.phoneNumber)))
    }

    suspend fun deleteContact(contact: ContactEntity) {
        dao.delete(contact)
    }

    suspend fun isNumberAllowed(rawNumber: String): Boolean {
        val normalized = PhoneUtils.normalize(rawNumber)
        return dao.findByNumber(normalized) != null
    }

    /** Usato dal CallBlockerService — non è un Flow perché serve una risposta immediata */
    suspend fun findByNumber(rawNumber: String): ContactEntity? {
        return dao.findByNumber(PhoneUtils.normalize(rawNumber))
    }

    suspend fun getAllContactsOnce(): List<ContactEntity> {
        // Convertiamo il Flow in List con first()
        return allContacts.let {
            var result = emptyList<ContactEntity>()
            it.collect { list -> result = list }
            result
        }
    }

    suspend fun replaceAll(contacts: List<ContactEntity>) {
        dao.deleteAll()
        dao.insertAll(contacts)
    }
}