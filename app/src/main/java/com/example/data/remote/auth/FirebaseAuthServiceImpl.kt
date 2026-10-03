package com.example.data.remote.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.domain.model.UserAccount
import com.example.domain.repository.AuthRepository
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirebaseAuthServiceImpl(private val appContext: Context) : AuthRepository {

    private val tag = "FirebaseAuthService"

    private val localGuestUser = MutableStateFlow<UserAccount?>(
        UserAccount(
            uid = "local_guest_" + UUID.randomUUID().toString().take(8),
            displayName = "Local Athlete",
            isAnonymous = true
        )
    )

    private val firebaseAuth: FirebaseAuth? by lazy {
        try {
            if (FirebaseApp.getApps(appContext).isNotEmpty()) {
                FirebaseAuth.getInstance()
            } else {
                null
            }
        } catch (e: Throwable) {
            Log.w(tag, "Firebase not available, running in local-first mode: ${e.message}")
            null
        }
    }

    override val currentUserFlow: Flow<UserAccount?> = callbackFlow {
        val auth = try { firebaseAuth } catch (e: Throwable) { null }
        if (auth == null) {
            trySend(localGuestUser.value)
            val job = launch {
                localGuestUser.collect { user ->
                    trySend(user)
                }
            }
            awaitClose { job.cancel() }
        } else {
            val listener = FirebaseAuth.AuthStateListener { firebaseAuthInstance ->
                try {
                    val fbUser = firebaseAuthInstance.currentUser
                    if (fbUser != null) {
                        trySend(
                            UserAccount(
                                uid = fbUser.uid,
                                email = fbUser.email,
                                displayName = fbUser.displayName ?: (if (fbUser.isAnonymous) "Guest Athlete" else "Athlete"),
                                photoUrl = fbUser.photoUrl?.toString(),
                                isAnonymous = fbUser.isAnonymous
                            )
                        )
                    } else {
                        trySend(localGuestUser.value)
                    }
                } catch (e: Throwable) {
                    trySend(localGuestUser.value)
                }
            }
            try {
                auth.addAuthStateListener(listener)
            } catch (e: Throwable) {
                trySend(localGuestUser.value)
            }
            awaitClose {
                try {
                    auth.removeAuthStateListener(listener)
                } catch (e: Throwable) {
                    // ignore
                }
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
                photoUrl = fbUser.photoUrl?.toString(),
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
                val existing = auth.currentUser
                if (existing != null) {
                    val user = UserAccount(
                        uid = existing.uid,
                        email = existing.email,
                        displayName = "Guest Athlete",
                        photoUrl = existing.photoUrl?.toString(),
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

    private fun resolveWebClientId(context: Context): String? {
        val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        if (resId != 0) {
            val id = context.getString(resId)
            if (id.isNotBlank()) return id
        }
        return null
    }

    override suspend fun signInWithGoogleCreds(context: Context): Result<UserAccount> {
        val auth = firebaseAuth
            ?: return Result.failure(IllegalStateException("Firebase Auth is not available. Please verify your internet and Firebase configuration."))

        val webClientId = resolveWebClientId(context)
            ?: return Result.failure(
                IllegalStateException(
                    "Google Web Client ID (default_web_client_id) not found. " +
                    "Ensure Google Sign-In is enabled in Firebase Console and a freshly generated google-services.json is placed in the app directory."
                )
            )

        val credentialManager = CredentialManager.create(context)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(
                request = request,
                context = context
            )

            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken

                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(firebaseCredential).await()
                val fbUser = authResult.user
                    ?: return Result.failure(IllegalStateException("Firebase user is null after authentication."))

                val account = UserAccount(
                    uid = fbUser.uid,
                    email = fbUser.email,
                    displayName = fbUser.displayName ?: googleIdTokenCredential.displayName ?: "Athlete",
                    photoUrl = fbUser.photoUrl?.toString() ?: googleIdTokenCredential.profilePictureUri?.toString(),
                    isAnonymous = false
                )
                localGuestUser.value = account
                Result.success(account)
            } else {
                Result.failure(IllegalStateException("Unexpected credential returned: ${credential.javaClass.simpleName}"))
            }
        } catch (e: Throwable) {
            val friendlyMessage = AuthErrorMapper.getUserMessage(e, context)
            Log.e(tag, "Google Sign-In failed: $friendlyMessage", e)
            Result.failure(Exception(friendlyMessage, e))
        }
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
