package com.example.data.local.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_stats")
data class DailyStatEntity(
    @PrimaryKey
    val dateString: String, // YYYY-MM-DD
    val activeMinutes: Int = 0,
    val totalJumps: Int = 0,
    val avgRate: Float = 0f,
    val sessionCount: Int = 0,
    val avgRpe: Float? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "weekly_stats")
data class WeeklyStatEntity(
    @PrimaryKey
    val yearWeekString: String, // YYYY-Www
    val activeMinutes: Int = 0,
    val totalJumps: Int = 0,
    val avgRate: Float = 0f,
    val sessionCount: Int = 0,
    val avgRpe: Float? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "monthly_stats")
data class MonthlyStatEntity(
    @PrimaryKey
    val yearMonthString: String, // YYYY-MM
    val activeMinutes: Int = 0,
    val totalJumps: Int = 0,
    val avgRate: Float = 0f,
    val sessionCount: Int = 0,
    val avgRpe: Float? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
