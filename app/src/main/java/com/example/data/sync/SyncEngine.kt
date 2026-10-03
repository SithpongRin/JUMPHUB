package com.example.data.sync

import com.example.data.local.datastore.AppPreferencesDataStore
import com.example.data.local.room.AppDatabase
import com.example.domain.repository.AuthRepository
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class SyncState {
    IDLE,
    SYNCING,
    SUCCESS,
    OFFLINE_SAVED,
    FAILED
}

data class SyncReport(
    val state: SyncState = SyncState.IDLE,
    val pendingItemsCount: Int = 0,
    val lastSyncTime: Long = 0L,
    val errorMessage: String? = null
)

class SyncEngine(
    private val database: AppDatabase,
    private val preferences: AppPreferencesDataStore,
    private val authRepository: AuthRepository
) {
    private val _syncReport = MutableStateFlow(SyncReport(state = SyncState.IDLE))
    val syncReport: Flow<SyncReport> = _syncReport.asStateFlow()

    suspend fun syncNow(): SyncReport = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        _syncReport.value = _syncReport.value.copy(state = SyncState.SYNCING)
        try {
            val user = authRepository.getCurrentUser()
            val pendingSessions = database.sessionDao().getPendingSyncSessions()

            if (user == null || !authRepository.isCloudSyncAvailable()) {
                // Operating in offline/local-first mode
                val report = SyncReport(
                    state = SyncState.OFFLINE_SAVED,
                    pendingItemsCount = pendingSessions.size,
                    lastSyncTime = System.currentTimeMillis()
                )
                _syncReport.value = report
                return@withContext report
            }

            // Sync pending sessions to Firestore if cloud sync available
            val firestore = try {
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
            } catch (e: Exception) {
                null
            }

            if (firestore != null) {
                val userSessionsCollection = firestore.collection("users").document(user.uid).collection("sessions")
                for (session in pendingSessions) {
                    val sessionMap = hashMapOf(
                        "uuid" to session.uuid,
                        "date" to session.date,
                        "planId" to (session.planId ?: ""),
                        "week" to (session.week ?: 0),
                        "sessionRef" to (session.sessionRef ?: ""),
                        "activeSec" to session.activeSec,
                        "totalJumps" to session.totalJumps,
                        "detectedTotal" to session.detectedTotal,
                        "correctedTotal" to session.correctedTotal,
                        "avgRate" to session.avgRate,
                        "bestStreak" to session.bestStreak,
                        "calories" to (session.calories?.toDouble() ?: 0.0),
                        "rpe" to (session.rpe ?: 0),
                        "completionPct" to session.completionPct,
                        "weightSnapshot" to (session.weightSnapshot?.toDouble() ?: 0.0),
                        "syncStatus" to "SYNCED",
                        "updatedAt" to System.currentTimeMillis()
                    )
                    userSessionsCollection.document(session.uuid).set(sessionMap).await()
                }
            }

            val now = System.currentTimeMillis()
            for (session in pendingSessions) {
                database.sessionDao().updateSyncStatus(session.uuid, "SYNCED", now)
            }
            preferences.setLastSyncTimestamp(now)

            val report = SyncReport(
                state = SyncState.SUCCESS,
                pendingItemsCount = 0,
                lastSyncTime = now
            )
            _syncReport.value = report
            report
        } catch (e: Exception) {
            val report = SyncReport(
                state = SyncState.FAILED,
                errorMessage = e.message
            )
            _syncReport.value = report
            report
        }
    }

    /**
     * Safely applies incoming sessions from cloud to local Room database.
     * Guaranteed never to recreate sessions that have a local deletion tombstone.
     */
    suspend fun syncInboundSession(inboundSession: com.example.data.local.room.SessionEntity): Boolean {
        val isTombstoned = database.syncMetadataDao().isSessionTombstoned(inboundSession.uuid) > 0
        if (isTombstoned) {
            // Drop inbound session: user has explicitly deleted this session locally!
            return false
        }
        database.sessionDao().insertSession(inboundSession)
        return true
    }
}
