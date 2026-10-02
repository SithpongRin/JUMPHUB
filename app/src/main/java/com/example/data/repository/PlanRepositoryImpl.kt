package com.example.data.repository

import com.example.data.local.room.AppDatabase
import com.example.data.local.room.UserPlanEntity
import com.example.domain.model.IntervalType
import com.example.domain.model.PlanInterval
import com.example.domain.model.TrainingPlan
import com.example.domain.model.UserActivePlan
import com.example.domain.repository.PlanRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.util.UUID

class PlanRepositoryImpl(private val database: AppDatabase) : PlanRepository {

    // Default seeded plans based on WHO physical activity recommendations (150-300 min moderate / 75-150 min vigorous per week)
    private val defaultPlans = listOf(
        TrainingPlan(
            id = "plan_health",
            titleResKey = "plan_health_title",
            descriptionResKey = "plan_health_desc",
            totalWeeks = 8,
            daysPerWeek = 3,
            category = "HEALTH",
            rounds = 8,
            workSeconds = 45,
            restSeconds = 45,
            targetJumps = 400,
            intervals = listOf(
                PlanInterval(IntervalType.WARMUP, durationSec = 180),
                PlanInterval(IntervalType.JUMP, durationSec = 45),
                PlanInterval(IntervalType.REST, durationSec = 45),
                PlanInterval(IntervalType.COOLDOWN, durationSec = 180)
            )
        ),
        TrainingPlan(
            id = "plan_endurance",
            titleResKey = "plan_endurance_title",
            descriptionResKey = "plan_endurance_desc",
            totalWeeks = 8,
            daysPerWeek = 4,
            category = "ENDURANCE",
            rounds = 10,
            workSeconds = 60,
            restSeconds = 30,
            targetJumps = 800,
            intervals = listOf(
                PlanInterval(IntervalType.WARMUP, durationSec = 180),
                PlanInterval(IntervalType.JUMP, durationSec = 60),
                PlanInterval(IntervalType.REST, durationSec = 30),
                PlanInterval(IntervalType.COOLDOWN, durationSec = 180)
            )
        ),
        TrainingPlan(
            id = "plan_fat_loss",
            titleResKey = "plan_fat_loss_title",
            descriptionResKey = "plan_fat_loss_desc",
            totalWeeks = 6,
            daysPerWeek = 4,
            category = "FAT_LOSS",
            rounds = 12,
            workSeconds = 30,
            restSeconds = 30,
            targetJumps = 600,
            intervals = listOf(
                PlanInterval(IntervalType.WARMUP, durationSec = 180),
                PlanInterval(IntervalType.JUMP, durationSec = 30),
                PlanInterval(IntervalType.REST, durationSec = 30),
                PlanInterval(IntervalType.COOLDOWN, durationSec = 180)
            )
        ),
        TrainingPlan(
            id = "plan_quick_10",
            titleResKey = "plan_quick_title",
            descriptionResKey = "plan_quick_desc",
            totalWeeks = 1,
            daysPerWeek = 5,
            category = "QUICK",
            rounds = 5,
            workSeconds = 60,
            restSeconds = 30,
            targetJumps = 450,
            intervals = listOf(
                PlanInterval(IntervalType.WARMUP, durationSec = 60),
                PlanInterval(IntervalType.JUMP, durationSec = 60),
                PlanInterval(IntervalType.REST, durationSec = 30),
                PlanInterval(IntervalType.COOLDOWN, durationSec = 60)
            )
        ),
        TrainingPlan(
            id = "plan_free_jump",
            titleResKey = "plan_free_jump_title",
            descriptionResKey = "plan_free_jump_desc",
            totalWeeks = 1,
            daysPerWeek = 7,
            category = "FREE",
            rounds = 1,
            workSeconds = 0,
            restSeconds = 0,
            targetJumps = 1000,
            intervals = emptyList()
        )
    )

    override fun getAvailablePlans(): Flow<List<TrainingPlan>> {
        return flowOf(defaultPlans)
    }

    override fun getActivePlan(): Flow<UserActivePlan?> {
        return database.planDao().getActiveUserPlan().map { entity ->
            entity?.let {
                UserActivePlan(
                    uuid = it.uuid,
                    planId = it.planId,
                    name = it.name,
                    currentWeek = it.currentWeek,
                    currentDay = it.currentDay,
                    status = it.status
                )
            }
        }
    }

    override suspend fun selectPlan(planId: String, name: String) {
        val now = System.currentTimeMillis()
        database.planDao().pauseActiveUserPlans(now)
        val userPlan = UserPlanEntity(
            uuid = UUID.randomUUID().toString(),
            planId = planId,
            name = name,
            currentWeek = 1,
            currentDay = 1,
            status = "ACTIVE",
            startedAt = now,
            updatedAt = now
        )
        database.planDao().insertUserPlan(userPlan)
    }

    override suspend fun updatePlanProgress(week: Int, day: Int) {
        // Updated in subsequent phases when active plan advances
    }

    override suspend fun getPlanById(planId: String): TrainingPlan? {
        return defaultPlans.find { it.id == planId }
    }
}
