package com.amlet.callblocker.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO (Data Access Object) for the whitelist contacts table.
 * Room generates the implementation at compile-time from these annotations.
 *
 * Flow<T> is a reactive stream: whenever the database changes the UI
 * recomposesa automatically without any polling.
 */
@Dao
interface ContactDao {

    @Query("SELECT * FROM allowed_contacts ORDER BY name ASC")
    fun getAllContacts(): Flow<List<ContactEntity>>

    @Query("SELECT COUNT(*) FROM allowed_contacts")
    fun getContactCount(): Flow<Int>

    /**
     * Looks up a number in the whitelist.
     * suspend = must be called from a coroutine.
     */
    @Query("SELECT * FROM allowed_contacts WHERE phoneNumber = :number LIMIT 1")
    suspend fun findByNumber(number: String): ContactEntity?

    @Query("SELECT * FROM allowed_contacts WHERE id = :id")
    suspend fun findById(id: Int): ContactEntity?

    /** OnConflict.REPLACE updates the record if it already exists. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contact: ContactEntity)

    @Update
    suspend fun update(contact: ContactEntity)

    @Delete
    suspend fun delete(contact: ContactEntity)

    /** Deletes all contacts — used by BackupManager before an import. */
    @Query("DELETE FROM allowed_contacts")
    suspend fun deleteAll()

    /** Bulk insert used when restoring from a backup. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(contacts: List<ContactEntity>)

    /** Synchronous count — for use in AppWidgets. */
    @Query("SELECT COUNT(*) FROM allowed_contacts")
    fun countSync(): Int
}