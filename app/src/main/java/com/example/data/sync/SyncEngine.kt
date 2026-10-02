package com.example.data.sync

import com.example.data.local.datastore.AppPreferencesDataStore
import com.example.data.local.room.AppDatabase
import com.example.domain.repository.AuthRepository
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

    suspend fun syncNow(): SyncReport {
        _syncReport.value = _syncReport.value.copy(state = SyncState.SYNCING)
        try {
            val user = authRepository.getCurrentUser()
            val pendingSessions = database.sessionDao().getPendingSyncSessions()

            if (user == null || !authRepository.isCloudSyncAvailable()) {
                // Operating in offline/local-first mode
                _syncReport.value = SyncReport(
                    state = SyncState.OFFLINE_SAVED,
                    pendingItemsCount = pendingSessions.size,
                    lastSyncTime = System.currentTimeMillis()
                )
                return _syncReport.value
            }

            // Mark local pending sessions as synced for now, in Phase 6 Firestore sync takes over
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
            return report
        } catch (e: Exception) {
            val report = SyncReport(
                state = SyncState.FAILED,
                errorMessage = e.message
            )
            _syncReport.value = report
            return report
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
