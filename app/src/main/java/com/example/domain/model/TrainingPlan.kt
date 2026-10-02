package com.example.domain.model

enum class IntervalType {
    WARMUP,
    JUMP,
    REST,
    COOLDOWN
}

data class PlanInterval(
    val type: IntervalType,
    val durationSec: Int? = null,
    val targetJumps: Int? = null
)

data class TrainingPlan(
    val id: String,
    val titleResKey: String,
    val descriptionResKey: String,
    val totalWeeks: Int,
    val daysPerWeek: Int,
    val category: String,
    val rounds: Int = 8,
    val workSeconds: Int = 30,
    val restSeconds: Int = 60,
    val targetJumps: Int = 500,
    val intervals: List<PlanInterval> = emptyList()
)

data class UserActivePlan(
    val uuid: String,
    val planId: String,
    val name: String,
    val currentWeek: Int,
    val currentDay: Int,
    val status: String
)
