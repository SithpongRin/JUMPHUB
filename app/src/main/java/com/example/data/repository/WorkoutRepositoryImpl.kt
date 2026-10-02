package com.example.data.repository

import com.example.data.local.room.AppDatabase
import com.example.data.local.room.RoundRecordEntity
import com.example.data.local.room.SessionEntity
import com.example.domain.model.RoundRecord
import com.example.domain.model.WorkoutSession
import com.example.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WorkoutRepositoryImpl(private val database: AppDatabase) : WorkoutRepository {

    override fun getAllSessions(): Flow<List<WorkoutSession>> {
        return database.sessionDao().getAllSessions().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getLatestSession(): Flow<WorkoutSession?> {
        return database.sessionDao().getLatestSession().map { it?.toDomain() }
    }

    override fun getTotalLifetimeJumps(): Flow<Int?> {
        return database.sessionDao().getTotalLifetimeJumps()
    }

    override fun getTotalLifetimeActiveSec(): Flow<Int?> {
        return database.sessionDao().getTotalLifetimeActiveSec()
    }

    override fun getTotalSessionCount(): Flow<Int> {
        return database.sessionDao().getTotalSessionCount()
    }

    override fun getSessionById(uuid: String): Flow<WorkoutSession?> {
        return database.sessionDao().getSessionByIdFlow(uuid).map { it?.toDomain() }
    }

    override suspend fun saveSession(session: WorkoutSession, rounds: List<RoundRecord>) {
        val sessionEntity = session.toEntity()
        database.sessionDao().insertSession(sessionEntity)
        val roundEntities = rounds.map { it.toEntity() }
        database.roundRecordDao().insertRounds(roundEntities)
    }

    override suspend fun getRoundsForSession(sessionId: String): List<RoundRecord> {
        return database.roundRecordDao().getRoundsForSessionSync(sessionId).map { it.toDomain() }
    }

    override suspend fun deleteSession(uuid: String) {
        // 1. Record sync tombstone for cloud sync
        val now = System.currentTimeMillis()
        database.syncMetadataDao().setValue(
            com.example.data.local.room.SyncMetadataEntity(
                key = "tombstone_session_$uuid",
                value = "$now",
                updatedAt = now
            )
        )
        // 2. Cascade delete records and session
        database.roundRecordDao().deleteRoundsForSession(uuid)
        database.sessionDao().deleteSessionById(uuid)
    }

    private fun SessionEntity.toDomain(): WorkoutSession = WorkoutSession(
        uuid = uuid,
        date = date,
        planId = planId,
        week = week,
        sessionRef = sessionRef,
        activeSec = activeSec,
        totalJumps = totalJumps,
        detectedTotal = detectedTotal,
        correctedTotal = correctedTotal,
        avgRate = avgRate,
        bestStreak = bestStreak,
        calories = calories,
        rpe = rpe,
        completionPct = completionPct,
        weightSnapshot = weightSnapshot,
        metUsed = metUsed,
        detectorVersion = detectorVersion,
        calibrationSnapshot = calibrationSnapshot,
        jumpTimestamps = jumpTimestamps,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncStatus = syncStatus
    )

    private fun WorkoutSession.toEntity(): SessionEntity = SessionEntity(
        uuid = uuid,
        date = date,
        planId = planId,
        week = week,
        sessionRef = sessionRef,
        activeSec = activeSec,
        totalJumps = totalJumps,
        detectedTotal = detectedTotal,
        correctedTotal = correctedTotal,
        avgRate = avgRate,
        bestStreak = bestStreak,
        calories = calories,
        rpe = rpe,
        completionPct = completionPct,
        weightSnapshot = weightSnapshot,
        metUsed = metUsed,
        detectorVersion = detectorVersion,
        calibrationSnapshot = calibrationSnapshot,
        jumpTimestamps = jumpTimestamps,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncStatus = syncStatus
    )

    private fun RoundRecordEntity.toDomain(): RoundRecord = RoundRecord(
        uuid = uuid,
        sessionId = sessionId,
        roundNo = roundNo,
        detectedJumps = detectedJumps,
        correctedJumps = correctedJumps,
        activeSec = activeSec,
        pausedSec = pausedSec
    )

    private fun RoundRecord.toEntity(): RoundRecordEntity = RoundRecordEntity(
        uuid = uuid,
        sessionId = sessionId,
        roundNo = roundNo,
        detectedJumps = detectedJumps,
        correctedJumps = correctedJumps,
        activeSec = activeSec,
        pausedSec = pausedSec
    )
}
