package com.amlet.callblocker.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedCallDao {

    /** Reactive stream: the UI updates automatically on every new block. */
    @Query("SELECT * FROM blocked_calls ORDER BY blockedAt DESC")
    fun getAllBlocked(): Flow<List<BlockedCallEntity>>

    /**
     * All call attempts for a specific number, newest first.
     * Applies the same normalisation as PhoneUtils.normalize() in SQL:
     * strips all non-digit characters (+, spaces, dashes) and leading zeros,
     * so that +39694807697, 39694807697, 0039694807697 all match each other.
     */
    @Query(
        "SELECT * FROM blocked_calls " +
        "WHERE LTRIM(REPLACE(REPLACE(REPLACE(phoneNumber, '+', ''), ' ', ''), '-', ''), '0') " +
        "    = LTRIM(REPLACE(REPLACE(REPLACE(:number,    '+', ''), ' ', ''), '-', ''), '0') " +
        "ORDER BY blockedAt DESC"
    )
    fun getCallsForNumber(number: String): Flow<List<BlockedCallEntity>>

    /** Total number of unique blocked numbers (for the stats card). */
    @Query("SELECT COUNT(DISTINCT phoneNumber) FROM blocked_calls")
    fun getBlockedCount(): Flow<Int>

    @Insert
    suspend fun insert(call: BlockedCallEntity)

    /** Deletes the entire log (used by "Clear log"). */
    @Query("DELETE FROM blocked_calls")
    suspend fun deleteAll()

    /** Deletes log entries older than the given Unix timestamp in ms. */
    @Query("DELETE FROM blocked_calls WHERE blockedAt < :beforeMs")
    suspend fun deleteOlderThan(beforeMs: Long)
}
