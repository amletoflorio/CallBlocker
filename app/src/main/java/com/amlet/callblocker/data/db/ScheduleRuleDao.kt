package com.amlet.callblocker.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleRuleDao {

    @Query("SELECT * FROM schedule_rules ORDER BY id ASC")
    fun getAll(): Flow<List<ScheduleRuleEntity>>

    @Query("SELECT * FROM schedule_rules WHERE enabled = 1 ORDER BY id ASC")
    suspend fun getEnabledRules(): List<ScheduleRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: ScheduleRuleEntity): Long

    @Delete
    suspend fun delete(rule: ScheduleRuleEntity)

    @Query("DELETE FROM schedule_rules")
    suspend fun deleteAll()
}
