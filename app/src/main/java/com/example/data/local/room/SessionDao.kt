package com.example.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY date DESC")
    fun getAllSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE uuid = :uuid LIMIT 1")
    suspend fun getSessionById(uuid: String): SessionEntity?

    @Query("SELECT * FROM sessions WHERE uuid = :uuid LIMIT 1")
    fun getSessionByIdFlow(uuid: String): Flow<SessionEntity?>

    @Query("SELECT * FROM sessions WHERE date >= :startOfDay AND date <= :endOfDay ORDER BY date DESC")
    fun getSessionsForDay(startOfDay: Long, endOfDay: Long): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE date >= :sinceTimestamp ORDER BY date DESC")
    fun getSessionsSince(sinceTimestamp: Long): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions ORDER BY date DESC LIMIT 1")
    fun getLatestSession(): Flow<SessionEntity?>

    @Query("SELECT * FROM sessions WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSyncSessions(): List<SessionEntity>

    @Query("SELECT SUM(totalJumps) FROM sessions")
    fun getTotalLifetimeJumps(): Flow<Int?>

    @Query("SELECT SUM(activeSec) FROM sessions")
    fun getTotalLifetimeActiveSec(): Flow<Int?>

    @Query("SELECT COUNT(*) FROM sessions")
    fun getTotalSessionCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<SessionEntity>)

    @Update
    suspend fun updateSession(session: SessionEntity)

    @Query("UPDATE sessions SET syncStatus = :status, updatedAt = :timestamp WHERE uuid = :uuid")
    suspend fun updateSyncStatus(uuid: String, status: String, timestamp: Long)

    @Query("DELETE FROM sessions WHERE uuid = :uuid")
    suspend fun deleteSessionById(uuid: String)

    @Query("DELETE FROM sessions")
    suspend fun clearAll()
}
