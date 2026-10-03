package com.example.data.repository

import com.example.data.local.room.AppDatabase
import com.example.data.local.room.BodyMetricEntity
import com.example.data.local.room.DailyStatEntity
import com.example.data.local.room.PersonalRecordEntity
import com.example.data.local.room.WeeklyStatEntity
import com.example.domain.model.BodyMetric
import com.example.domain.model.PersonalRecord
import com.example.domain.model.RecordType
import com.example.domain.repository.ProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class ProgressRepositoryImpl(private val database: AppDatabase) : ProgressRepository {

    override fun getDailyStats(): Flow<List<DailyStatEntity>> {
        return database.statsDao().getLast7DaysStats()
    }

    override fun getWeeklyStats(): Flow<List<WeeklyStatEntity>> {
        return database.statsDao().getLast12WeeksStats()
    }

    override fun getPersonalRecords(): Flow<List<PersonalRecord>> {
        return database.personalRecordDao().getAllRecords().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getLatestWeight(): Flow<BodyMetric?> {
        return database.bodyMetricDao().getLatestMetric().map { entity ->
            entity?.let {
                BodyMetric(
                    uuid = it.uuid,
                    date = it.date,
                    weightKg = it.weightKg,
                    waistCm = it.waistCm
                )
            }
        }
    }

    override fun getAllMetrics(): Flow<List<BodyMetric>> {
        return database.bodyMetricDao().getAllMetrics().map { entities ->
            entities.map {
                BodyMetric(
                    uuid = it.uuid,
                    date = it.date,
                    weightKg = it.weightKg,
                    waistCm = it.waistCm
                )
            }
        }
    }

    override suspend fun logWeight(weightKg: Float, waistCm: Float?) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val metric = BodyMetricEntity(
            uuid = UUID.randomUUID().toString(),
            date = now,
            weightKg = weightKg,
            waistCm = waistCm,
            createdAt = now,
            updatedAt = now
        )
        database.bodyMetricDao().insertMetric(metric)
    }

    override suspend fun saveRecord(record: PersonalRecord) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val entity = PersonalRecordEntity(
            uuid = record.uuid,
            type = record.type.name,
            value = record.value,
            previousValue = record.previousValue,
            date = record.date,
            sessionId = record.sessionId
        )
        database.personalRecordDao().insertRecord(entity)
    }

    private fun PersonalRecordEntity.toDomain(): PersonalRecord = PersonalRecord(
        uuid = uuid,
        type = try { RecordType.valueOf(type) } catch (e: Exception) { RecordType.LONGEST_STREAK },
        value = value,
        previousValue = previousValue,
        date = date,
        sessionId = sessionId
    )
}
