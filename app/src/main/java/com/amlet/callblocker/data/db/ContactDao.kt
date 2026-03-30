package com.amlet.callblocker.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO = Data Access Object.
 * Room genera l'implementazione a compile-time dalle annotazioni.
 *
 * Flow<T> = stream reattivo: ogni volta che il DB cambia,
 * la UI riceve automaticamente i nuovi dati senza polling.
 * È analogo a un Observable/Subject in RxJS o a un generator in Python.
 */
@Dao
interface ContactDao {

    @Query("SELECT * FROM allowed_contacts ORDER BY name ASC")
    fun getAllContacts(): Flow<List<ContactEntity>>

    @Query("SELECT COUNT(*) FROM allowed_contacts")
    fun getContactCount(): Flow<Int>

    /**
     * Cerca un numero nella whitelist.
     * `suspend` = funzione asincrona (deve essere chiamata da una coroutine)
     */
    @Query("SELECT * FROM allowed_contacts WHERE phoneNumber = :number LIMIT 1")
    suspend fun findByNumber(number: String): ContactEntity?

    @Query("SELECT * FROM allowed_contacts WHERE id = :id")
    suspend fun findById(id: Int): ContactEntity?

    /** OnConflict.REPLACE aggiorna il record se esiste già */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contact: ContactEntity)

    @Update
    suspend fun update(contact: ContactEntity)

    @Delete
    suspend fun delete(contact: ContactEntity)

    /** Usato dal BackupManager prima di un import */
    @Query("DELETE FROM allowed_contacts")
    suspend fun deleteAll()

    /** Bulk insert per il ripristino da backup */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(contacts: List<ContactEntity>)
}