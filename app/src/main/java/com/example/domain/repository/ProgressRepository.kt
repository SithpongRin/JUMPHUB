package com.example.domain.repository

import com.example.data.local.room.DailyStatEntity
import com.example.data.local.room.WeeklyStatEntity
import com.example.domain.model.BodyMetric
import com.example.domain.model.PersonalRecord
import kotlinx.coroutines.flow.Flow

interface ProgressRepository {
    fun getDailyStats(): Flow<List<DailyStatEntity>>
    fun getWeeklyStats(): Flow<List<WeeklyStatEntity>>
    fun getPersonalRecords(): Flow<List<PersonalRecord>>
    fun getLatestWeight(): Flow<BodyMetric?>
    fun getAllMetrics(): Flow<List<BodyMetric>>
    suspend fun logWeight(weightKg: Float, waistCm: Float?)
    suspend fun saveRecord(record: PersonalRecord)
}
