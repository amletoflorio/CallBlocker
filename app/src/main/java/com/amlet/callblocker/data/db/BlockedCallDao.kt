package com.amlet.callblocker.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedCallDao {

    /** Stream reattivo: la UI si aggiorna automaticamente ad ogni nuovo blocco */
    @Query("SELECT * FROM blocked_calls ORDER BY blockedAt DESC")
    fun getAllBlocked(): Flow<List<BlockedCallEntity>>

    @Query("SELECT COUNT(*) FROM blocked_calls")
    fun getBlockedCount(): Flow<Int>

    @Insert
    suspend fun insert(call: BlockedCallEntity)

    /** Cancella tutto il log (usato da "Svuota log") */
    @Query("DELETE FROM blocked_calls")
    suspend fun deleteAll()
}
