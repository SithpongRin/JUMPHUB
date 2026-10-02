package com.example.data.local.room

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "personal_records",
    indices = [
        Index(value = ["type"])
    ]
)
data class PersonalRecordEntity(
    @PrimaryKey
    val uuid: String,
    val type: String, // LONGEST_STREAK, MOST_30S, MOST_60S, MOST_2M, MOST_5M, MOST_SESSION, PEAK_RATE
    val value: Float,
    val previousValue: Float = 0f,
    val date: Long = System.currentTimeMillis(),
    val sessionId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "PENDING"
)
