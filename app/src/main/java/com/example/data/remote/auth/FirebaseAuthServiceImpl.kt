package com.example.data.remote.auth

import android.content.Context
import android.util.Log
import com.example.domain.model.UserAccount
import com.example.domain.repository.AuthRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import java.util.UUID

class FirebaseAuthServiceImpl(private val context: Context) : AuthRepository {

    private val localGuestUser = MutableStateFlow<UserAccount?>(
        UserAccount(
            uid = "local_guest_" + UUID.randomUUID().toString().take(8),
            displayName = "Local Athlete",
            isAnonymous = true
        )
    )

    private val firebaseAuth: FirebaseAuth? by lazy {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseAuth.getInstance()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w("FirebaseAuthService", "Firebase not available, running in local-first mode: ${e.message}")
            null
        }
    }

    override val currentUserFlow: Flow<UserAccount?> = callbackFlow {
        val auth = firebaseAuth
        if (auth == null) {
            // Local-first guest flow
            localGuestUser.collect { user ->
                trySend(user)
            }
            awaitClose { }
        } else {
            val listener = FirebaseAuth.AuthStateListener { firebaseAuthInstance ->
                val fbUser = firebaseAuthInstance.currentUser
                if (fbUser != null) {
                    trySend(
                        UserAccount(
                            uid = fbUser.uid,
                            email = fbUser.email,
                            displayName = fbUser.displayName ?: (if (fbUser.isAnonymous) "Guest Athlete" else "Athlete"),
                            isAnonymous = fbUser.isAnonymous
                        )
                    )
                } else {
                    trySend(localGuestUser.value)
                }
            }
            auth.addAuthStateListener(listener)
            awaitClose {
                auth.removeAuthStateListener(listener)
            }
        }
    }

    override fun getCurrentUser(): UserAccount? {
        val fbUser = firebaseAuth?.currentUser
        return if (fbUser != null) {
            UserAccount(
                uid = fbUser.uid,
                email = fbUser.email,
                displayName = fbUser.displayName ?: (if (fbUser.isAnonymous) "Guest Athlete" else "Athlete"),
                isAnonymous = fbUser.isAnonymous
            )
        } else {
            localGuestUser.value
        }
    }

    override suspend fun signInAsGuest(): Result<UserAccount> {
        val auth = firebaseAuth
        return if (auth != null) {
            try {
                // If already signed in, return current
                val existing = auth.currentUser
                if (existing != null) {
                    val user = UserAccount(
                        uid = existing.uid,
                        email = existing.email,
                        displayName = "Guest Athlete",
                        isAnonymous = true
                    )
                    Result.success(user)
                } else {
                    val user = localGuestUser.value ?: UserAccount(
                        uid = "local_guest_" + UUID.randomUUID().toString().take(8),
                        displayName = "Guest Athlete",
                        isAnonymous = true
                    )
                    localGuestUser.value = user
                    Result.success(user)
                }
            } catch (e: Exception) {
                val fallback = localGuestUser.value ?: UserAccount(
                    uid = "local_guest_" + UUID.randomUUID().toString().take(8),
                    displayName = "Guest Athlete",
                    isAnonymous = true
                )
                localGuestUser.value = fallback
                Result.success(fallback)
            }
        } else {
            val fallback = localGuestUser.value ?: UserAccount(
                uid = "local_guest_" + UUID.randomUUID().toString().take(8),
                displayName = "Guest Athlete",
                isAnonymous = true
            )
            localGuestUser.value = fallback
            Result.success(fallback)
        }
    }

    override suspend fun signInWithGoogleAccount(email: String, displayName: String): Result<UserAccount> {
        val user = UserAccount(
            uid = "google_" + kotlin.math.abs(email.hashCode()),
            email = email,
            displayName = displayName.ifBlank { email.substringBefore("@") },
            isAnonymous = false
        )
        localGuestUser.value = user
        return Result.success(user)
    }

    override suspend fun signOut(): Result<Unit> {
        return try {
            firebaseAuth?.signOut()
            localGuestUser.value = UserAccount(
                uid = "local_guest_" + UUID.randomUUID().toString().take(8),
                displayName = "Guest Athlete",
                isAnonymous = true
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun isCloudSyncAvailable(): Boolean {
        return (firebaseAuth?.currentUser != null && !firebaseAuth!!.currentUser!!.isAnonymous) || 
               (localGuestUser.value != null && !localGuestUser.value!!.isAnonymous)
    }
}
