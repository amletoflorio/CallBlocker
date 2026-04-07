package com.amlet.callblocker.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedCallDao {

    @Query("SELECT * FROM blocked_calls ORDER BY blockedAt DESC")
    fun getAllBlocked(): Flow<List<BlockedCallEntity>>

    @Query(
        "SELECT * FROM blocked_calls " +
                "WHERE LTRIM(REPLACE(REPLACE(REPLACE(phoneNumber, '+', ''), ' ', ''), '-', ''), '0') " +
                "    = LTRIM(REPLACE(REPLACE(REPLACE(:number,    '+', ''), ' ', ''), '-', ''), '0') " +
                "ORDER BY blockedAt DESC"
    )
    fun getCallsForNumber(number: String): Flow<List<BlockedCallEntity>>

    @Query("SELECT COUNT(DISTINCT phoneNumber) FROM blocked_calls")
    fun getBlockedCount(): Flow<Int>

    @Query(
        "SELECT COUNT(*) FROM blocked_calls " +
                "WHERE LTRIM(REPLACE(REPLACE(REPLACE(phoneNumber, '+', ''), ' ', ''), '-', ''), '0') " +
                "    = LTRIM(REPLACE(REPLACE(REPLACE(:number,    '+', ''), ' ', ''), '-', ''), '0') " +
                "AND callType = 'incoming' " +
                "AND blockedAt >= :sinceMs"
    )
    suspend fun countCallsSince(number: String, sinceMs: Long): Int

    /** Inserts a record and returns the generated row ID. Used to update simSlot later. */
    @Insert
    suspend fun insertAndGetId(call: BlockedCallEntity): Long

    /** Updates the simSlot for a specific row — called after CallLog lookup on MIUI. */
    @Query("UPDATE blocked_calls SET simSlot = :simSlot WHERE id = :id")
    suspend fun updateSimSlot(id: Long, simSlot: String)

    @Insert
    suspend fun insert(call: BlockedCallEntity)

    @Query("DELETE FROM blocked_calls")
    suspend fun deleteAll()

    @Query("DELETE FROM blocked_calls WHERE blockedAt < :beforeMs")
    suspend fun deleteOlderThan(beforeMs: Long)

    /** Synchronous count — for use in AppWidgets (not in coroutines). */
    @Query("SELECT COUNT(DISTINCT phoneNumber) FROM blocked_calls")
    fun countBlockedSync(): Int

    /** Returns the most recent blocked call entry, or null — for use in AppWidgets. */
    @Query("SELECT * FROM blocked_calls ORDER BY blockedAt DESC LIMIT 1")
    fun getLastBlockedSync(): BlockedCallEntity?
}