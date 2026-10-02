package com.example.data.local.room

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "round_records",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["uuid"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["sessionId", "roundNo"], unique = true)
    ]
)
data class RoundRecordEntity(
    @PrimaryKey
    val uuid: String,
    val sessionId: String,
    val roundNo: Int,
    val detectedJumps: Int = 0,
    val correctedJumps: Int = 0,
    val activeSec: Int = 0,
    val pausedSec: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "PENDING"
)
