package com.example.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanDao {
    @Query("SELECT * FROM plan_definitions")
    fun getAllPlanDefinitions(): Flow<List<PlanDefinitionEntity>>

    @Query("SELECT * FROM plan_definitions WHERE id = :id LIMIT 1")
    suspend fun getPlanDefinitionById(id: String): PlanDefinitionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanDefinitions(plans: List<PlanDefinitionEntity>)

    @Query("SELECT * FROM user_plans WHERE status = 'ACTIVE' LIMIT 1")
    fun getActiveUserPlan(): Flow<UserPlanEntity?>

    @Query("SELECT * FROM user_plans ORDER BY startedAt DESC")
    fun getAllUserPlans(): Flow<List<UserPlanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserPlan(userPlan: UserPlanEntity)

    @Query("UPDATE user_plans SET currentWeek = :week, currentDay = :day, updatedAt = :timestamp WHERE uuid = :uuid")
    suspend fun updateUserPlanProgress(uuid: String, week: Int, day: Int, timestamp: Long)

    @Query("UPDATE user_plans SET status = 'PAUSED', updatedAt = :timestamp WHERE status = 'ACTIVE'")
    suspend fun pauseActiveUserPlans(timestamp: Long)
}
