package com.example.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RoundRecordDao {
    @Query("SELECT * FROM round_records WHERE sessionId = :sessionId ORDER BY roundNo ASC")
    fun getRoundsForSession(sessionId: String): Flow<List<RoundRecordEntity>>

    @Query("SELECT * FROM round_records WHERE sessionId = :sessionId ORDER BY roundNo ASC")
    suspend fun getRoundsForSessionSync(sessionId: String): List<RoundRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRounds(rounds: List<RoundRecordEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRound(round: RoundRecordEntity)

    @Query("DELETE FROM round_records WHERE sessionId = :sessionId")
    suspend fun deleteRoundsForSession(sessionId: String)
}
