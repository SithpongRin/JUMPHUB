package com.example.domain.repository

import com.example.domain.model.TrainingPlan
import com.example.domain.model.UserActivePlan
import kotlinx.coroutines.flow.Flow

interface PlanRepository {
    fun getAvailablePlans(): Flow<List<TrainingPlan>>
    fun getActivePlan(): Flow<UserActivePlan?>
    suspend fun selectPlan(planId: String, name: String)
    suspend fun updatePlanProgress(week: Int, day: Int)
}
