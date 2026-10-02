package com.example.data.local.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plan_definitions")
data class PlanDefinitionEntity(
    @PrimaryKey
    val id: String,
    val titleKey: String,
    val descriptionKey: String,
    val totalWeeks: Int,
    val daysPerWeek: Int,
    val category: String, // HEALTH, ENDURANCE, FAT_LOSS, QUICK, FREE
    val intervalsJson: String = ""
)

@Entity(tableName = "user_plans")
data class UserPlanEntity(
    @PrimaryKey
    val uuid: String,
    val planId: String,
    val name: String,
    val currentWeek: Int = 1,
    val currentDay: Int = 1,
    val status: String = "ACTIVE", // ACTIVE, COMPLETED, PAUSED
    val startedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "PENDING"
)
