package com.example.data

import android.content.Context
import com.example.data.local.datastore.AppPreferencesDataStore
import com.example.data.local.room.AppDatabase
import com.example.data.remote.auth.FirebaseAuthServiceImpl
import com.example.data.repository.PlanRepositoryImpl
import com.example.data.repository.ProgressRepositoryImpl
import com.example.data.repository.WorkoutRepositoryImpl
import com.example.data.sync.SyncEngine
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.PlanRepository
import com.example.domain.repository.ProgressRepository
import com.example.domain.repository.WorkoutRepository

class AppContainer(context: Context) {
    val database: AppDatabase by lazy { AppDatabase.getInstance(context) }
    val preferences: AppPreferencesDataStore by lazy { AppPreferencesDataStore(context) }
    val authRepository: AuthRepository by lazy { FirebaseAuthServiceImpl(context) }
    val workoutRepository: WorkoutRepository by lazy { WorkoutRepositoryImpl(database) }
    val planRepository: PlanRepository by lazy { PlanRepositoryImpl(database) }
    val progressRepository: ProgressRepository by lazy { ProgressRepositoryImpl(database) }
    val syncEngine: SyncEngine by lazy { SyncEngine(database, preferences, authRepository) }
}
