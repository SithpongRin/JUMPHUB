package com.example.data.local.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminder_schedules")
data class ReminderScheduleEntity(
    @PrimaryKey
    val uuid: String,
    val planId: String,
    val dayOfWeek: Int, // 1 = Monday ... 7 = Sunday
    val timeHour: Int = 18,
    val timeMinute: Int = 0,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "PENDING"
)
