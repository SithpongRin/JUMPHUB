package com.example.domain.repository

import com.example.domain.model.UserAccount
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUserFlow: Flow<UserAccount?>
    fun getCurrentUser(): UserAccount?
    suspend fun signInAsGuest(): Result<UserAccount>
    suspend fun signInWithGoogleAccount(email: String, displayName: String): Result<UserAccount>
    suspend fun signInWithGoogleCreds(context: android.content.Context): Result<UserAccount>
    suspend fun signOut(): Result<Unit>
    fun isCloudSyncAvailable(): Boolean
}
