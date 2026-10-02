package com.example.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BodyMetricDao {
    @Query("SELECT * FROM body_metrics ORDER BY date DESC")
    fun getAllMetrics(): Flow<List<BodyMetricEntity>>

    @Query("SELECT * FROM body_metrics ORDER BY date DESC LIMIT 1")
    fun getLatestMetric(): Flow<BodyMetricEntity?>

    @Query("SELECT * FROM body_metrics WHERE date <= :targetDate ORDER BY date DESC LIMIT 1")
    suspend fun getMetricOnOrBeforeDate(targetDate: Long): BodyMetricEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetric(metric: BodyMetricEntity)

    @Query("DELETE FROM body_metrics WHERE uuid = :uuid")
    suspend fun deleteMetricById(uuid: String)
}
