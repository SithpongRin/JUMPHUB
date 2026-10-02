package com.example.data.local.room

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "body_metrics",
    indices = [
        Index(value = ["date"], unique = true)
    ]
)
data class BodyMetricEntity(
    @PrimaryKey
    val uuid: String,
    val date: Long, // timestamp ms start of day
    val weightKg: Float,
    val waistCm: Float? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "PENDING"
)
