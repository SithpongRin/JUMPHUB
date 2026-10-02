package com.example.domain.repository

import com.example.domain.model.RoundRecord
import com.example.domain.model.WorkoutSession
import kotlinx.coroutines.flow.Flow

interface WorkoutRepository {
    fun getAllSessions(): Flow<List<WorkoutSession>>
    fun getLatestSession(): Flow<WorkoutSession?>
    fun getTotalLifetimeJumps(): Flow<Int?>
    fun getTotalLifetimeActiveSec(): Flow<Int?>
    fun getTotalSessionCount(): Flow<Int>
    fun getSessionById(uuid: String): Flow<WorkoutSession?>
    suspend fun saveSession(session: WorkoutSession, rounds: List<RoundRecord>)
    suspend fun getRoundsForSession(sessionId: String): List<RoundRecord>
    suspend fun deleteSession(uuid: String)
}
