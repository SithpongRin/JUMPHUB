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

        // 3. Recompute affected daily/weekly/monthly statistics & personal records
        recomputeStatsAndRecords()
    }

    private suspend fun recomputeStatsAndRecords() {
        val remaining = database.sessionDao().getAllSessionsSync()

        // A. Clear existing aggregate stats and personal records
        database.statsDao().clearDailyStats()
        database.statsDao().clearWeeklyStats()
        database.statsDao().clearMonthlyStats()
        database.personalRecordDao().clearRecords()

        if (remaining.isEmpty()) return

        // B. Recompute daily stats
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val weekFormat = java.text.SimpleDateFormat("yyyy-'W'ww", java.util.Locale.US)
        val monthFormat = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.US)

        val byDate = remaining.groupBy { dateFormat.format(java.util.Date(it.date)) }
        for ((dateStr, sessions) in byDate) {
            val totalMins = sessions.sumOf { it.activeSec } / 60
            val totalJumps = sessions.sumOf { it.totalJumps }
            val avgRate = if (sessions.isNotEmpty()) sessions.map { it.avgRate }.average().toFloat() else 0f
            val rpeList = sessions.mapNotNull { it.rpe }
            val avgRpe = if (rpeList.isNotEmpty()) rpeList.average().toFloat() else null

            database.statsDao().insertDailyStat(
                com.example.data.local.room.DailyStatEntity(
                    dateString = dateStr,
                    activeMinutes = totalMins,
                    totalJumps = totalJumps,
                    avgRate = avgRate,
                    sessionCount = sessions.size,
                    avgRpe = avgRpe
                )
            )
        }

        // C. Recompute weekly stats
        val byWeek = remaining.groupBy { weekFormat.format(java.util.Date(it.date)) }
        for ((weekStr, sessions) in byWeek) {
            val totalMins = sessions.sumOf { it.activeSec } / 60
            val totalJumps = sessions.sumOf { it.totalJumps }
            val avgRate = if (sessions.isNotEmpty()) sessions.map { it.avgRate }.average().toFloat() else 0f
            val rpeList = sessions.mapNotNull { it.rpe }
            val avgRpe = if (rpeList.isNotEmpty()) rpeList.average().toFloat() else null

            database.statsDao().insertWeeklyStat(
                com.example.data.local.room.WeeklyStatEntity(
                    yearWeekString = weekStr,
                    activeMinutes = totalMins,
                    totalJumps = totalJumps,
                    avgRate = avgRate,
                    sessionCount = sessions.size,
                    avgRpe = avgRpe
                )
            )
        }

        // D. Recompute monthly stats
        val byMonth = remaining.groupBy { monthFormat.format(java.util.Date(it.date)) }
        for ((monthStr, sessions) in byMonth) {
            val totalMins = sessions.sumOf { it.activeSec } / 60
            val totalJumps = sessions.sumOf { it.totalJumps }
            val avgRate = if (sessions.isNotEmpty()) sessions.map { it.avgRate }.average().toFloat() else 0f
            val rpeList = sessions.mapNotNull { it.rpe }
            val avgRpe = if (rpeList.isNotEmpty()) rpeList.average().toFloat() else null

            database.statsDao().insertMonthlyStat(
                com.example.data.local.room.MonthlyStatEntity(
                    yearMonthString = monthStr,
                    activeMinutes = totalMins,
                    totalJumps = totalJumps,
                    avgRate = avgRate,
                    sessionCount = sessions.size,
                    avgRpe = avgRpe
                )
            )
        }

        // E. Recompute Personal Records from remaining sessions
        val maxSession = remaining.maxByOrNull { it.totalJumps }
        if (maxSession != null && maxSession.totalJumps > 0) {
            database.personalRecordDao().insertRecord(
                com.example.data.local.room.PersonalRecordEntity(
                    uuid = java.util.UUID.randomUUID().toString(),
                    type = com.example.domain.model.RecordType.MOST_SESSION.name,
                    value = maxSession.totalJumps.toFloat(),
                    sessionId = maxSession.uuid,
                    date = maxSession.date
                )
            )
        }

        val maxStreak = remaining.maxByOrNull { it.bestStreak }
        if (maxStreak != null && maxStreak.bestStreak > 0) {
            database.personalRecordDao().insertRecord(
                com.example.data.local.room.PersonalRecordEntity(
                    uuid = java.util.UUID.randomUUID().toString(),
                    type = com.example.domain.model.RecordType.LONGEST_STREAK.name,
                    value = maxStreak.bestStreak.toFloat(),
                    sessionId = maxStreak.uuid,
                    date = maxStreak.date
                )
            )
        }

        val maxCadence = remaining.maxByOrNull { it.avgRate }
        if (maxCadence != null && maxCadence.avgRate > 0) {
            database.personalRecordDao().insertRecord(
                com.example.data.local.room.PersonalRecordEntity(
                    uuid = java.util.UUID.randomUUID().toString(),
                    type = com.example.domain.model.RecordType.PEAK_RATE.name,
                    value = maxCadence.avgRate,
                    sessionId = maxCadence.uuid,
                    date = maxCadence.date
                )
            )
        }
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
