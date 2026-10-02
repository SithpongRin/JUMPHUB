package com.example.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonalRecordDao {
    @Query("SELECT * FROM personal_records ORDER BY type ASC, date DESC")
    fun getAllRecords(): Flow<List<PersonalRecordEntity>>

    @Query("SELECT * FROM personal_records WHERE type = :type ORDER BY value DESC LIMIT 1")
    suspend fun getBestRecordForType(type: String): PersonalRecordEntity?

    @Query("SELECT * FROM personal_records WHERE type = :type ORDER BY value DESC LIMIT 1")
    fun getBestRecordFlow(type: String): Flow<PersonalRecordEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: PersonalRecordEntity)

    @Query("DELETE FROM personal_records WHERE uuid = :uuid")
    suspend fun deleteRecordById(uuid: String)
}
