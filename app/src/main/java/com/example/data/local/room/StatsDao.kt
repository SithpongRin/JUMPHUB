package com.example.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StatsDao {
    @Query("SELECT * FROM daily_stats ORDER BY dateString DESC")
    fun getAllDailyStats(): Flow<List<DailyStatEntity>>

    @Query("SELECT * FROM daily_stats ORDER BY dateString DESC LIMIT 7")
    fun getLast7DaysStats(): Flow<List<DailyStatEntity>>

    @Query("SELECT * FROM weekly_stats ORDER BY yearWeekString DESC LIMIT 12")
    fun getLast12WeeksStats(): Flow<List<WeeklyStatEntity>>

    @Query("SELECT * FROM monthly_stats ORDER BY yearMonthString DESC LIMIT 12")
    fun getLast12MonthsStats(): Flow<List<MonthlyStatEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyStat(stat: DailyStatEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeeklyStat(stat: WeeklyStatEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMonthlyStat(stat: MonthlyStatEntity)

    @Query("DELETE FROM daily_stats")
    suspend fun clearDailyStats()

    @Query("DELETE FROM weekly_stats")
    suspend fun clearWeeklyStats()

    @Query("DELETE FROM monthly_stats")
    suspend fun clearMonthlyStats()
}
