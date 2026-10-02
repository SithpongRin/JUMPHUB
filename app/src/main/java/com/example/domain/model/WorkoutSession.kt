package com.example.domain.model

data class WorkoutSession(
    val uuid: String,
    val date: Long,
    val planId: String? = null,
    val week: Int? = null,
    val sessionRef: String? = null,
    val activeSec: Int = 0,
    val totalJumps: Int = 0,
    val detectedTotal: Int = 0,
    val correctedTotal: Int = 0,
    val avgRate: Float = 0f,
    val bestStreak: Int = 0,
    val calories: Float? = null,
    val rpe: Int? = null,
    val completionPct: Float = 100f,
    val weightSnapshot: Float? = null,
    val metUsed: Float? = 11.5f,
    val detectorVersion: String = "1.0",
    val calibrationSnapshot: String? = null,
    val jumpTimestamps: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "PENDING"
)

data class RoundRecord(
    val uuid: String,
    val sessionId: String,
    val roundNo: Int,
    val detectedJumps: Int = 0,
    val correctedJumps: Int = 0,
    val activeSec: Int = 0,
    val pausedSec: Int = 0
)
