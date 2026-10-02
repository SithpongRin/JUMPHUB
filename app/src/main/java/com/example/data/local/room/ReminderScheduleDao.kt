package com.example.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderScheduleDao {
    @Query("SELECT * FROM reminder_schedules WHERE isEnabled = 1")
    fun getActiveSchedules(): Flow<List<ReminderScheduleEntity>>

    @Query("SELECT * FROM reminder_schedules WHERE planId = :planId")
    fun getSchedulesForPlan(planId: String): Flow<List<ReminderScheduleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedules(schedules: List<ReminderScheduleEntity>)

    @Query("DELETE FROM reminder_schedules WHERE planId = :planId")
    suspend fun deleteSchedulesForPlan(planId: String)
}
