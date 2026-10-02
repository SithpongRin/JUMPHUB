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
    val database: AppDatabase = AppDatabase.getInstance(context)
    val preferences: AppPreferencesDataStore = AppPreferencesDataStore(context)
    val authRepository: AuthRepository = FirebaseAuthServiceImpl(context)
    val workoutRepository: WorkoutRepository = WorkoutRepositoryImpl(database)
    val planRepository: PlanRepository = PlanRepositoryImpl(database)
    val progressRepository: ProgressRepository = ProgressRepositoryImpl(database)
    val syncEngine: SyncEngine = SyncEngine(database, preferences, authRepository)
}
