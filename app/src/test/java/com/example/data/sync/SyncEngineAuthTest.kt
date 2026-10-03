package com.example.data.sync

import com.example.data.local.datastore.AppPreferencesDataStore
import com.example.data.local.room.AppDatabase
import com.example.domain.model.UserAccount
import com.example.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

class FakeAuthRepository(
    private var currentUser: UserAccount? = null,
    private var isCloudSyncAvailableResult: Boolean = false
) : AuthRepository {
    override val currentUserFlow: Flow<UserAccount?> = flowOf(currentUser)
    override fun getCurrentUser(): UserAccount? = currentUser
    override suspend fun signInAsGuest(): Result<UserAccount> {
        val guest = UserAccount(uid = "guest_123", isAnonymous = true)
        currentUser = guest
        return Result.success(guest)
    }
    override suspend fun signInWithGoogleAccount(email: String, displayName: String): Result<UserAccount> {
        val user = UserAccount(uid = "google_123", email = email, displayName = displayName, isAnonymous = false)
        currentUser = user
        return Result.success(user)
    }
    override suspend fun signInWithGoogleCreds(context: android.content.Context): Result<UserAccount> {
        val user = UserAccount(uid = "google_creds_123", email = "test@jumphub.com", displayName = "Test Athlete", isAnonymous = false)
        currentUser = user
        return Result.success(user)
    }
    override suspend fun signOut(): Result<Unit> {
        currentUser = null
        return Result.success(Unit)
    }
    override fun isCloudSyncAvailable(): Boolean = isCloudSyncAvailableResult

    fun setUser(user: UserAccount?, cloudSyncAvailable: Boolean) {
        currentUser = user
        isCloudSyncAvailableResult = cloudSyncAvailable
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SyncEngineAuthTest {

    private lateinit var database: AppDatabase
    private lateinit var preferences: AppPreferencesDataStore
    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var syncEngine: SyncEngine

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        database = AppDatabase.getInstance(context)
        preferences = AppPreferencesDataStore(context)
        fakeAuthRepository = FakeAuthRepository()
        syncEngine = SyncEngine(database, preferences, fakeAuthRepository)
    }

    @Test
    fun `syncNow works fully offline when not signed in`() = runBlocking {
        // User is anonymous or null, cloud sync not available
        fakeAuthRepository.setUser(UserAccount(uid = "local_guest_1", isAnonymous = true), cloudSyncAvailable = false)

        val report = syncEngine.syncNow()

        assertEquals(SyncState.OFFLINE_SAVED, report.state)
        assertTrue(report.lastSyncTime > 0)
    }

    @Test
    fun `syncInboundSession drops session if tombstone exists`() = runBlocking {
        val sessionUuid = "session_deleted_locally"
        database.syncMetadataDao().setValue(
            com.example.data.local.room.SyncMetadataEntity(
                key = "tombstone_session_$sessionUuid",
                value = "DELETED"
            )
        )

        val inbound = com.example.data.local.room.SessionEntity(
            uuid = sessionUuid,
            date = System.currentTimeMillis(),
            totalJumps = 200,
            detectedTotal = 200,
            correctedTotal = 200,
            activeSec = 120,
            avgRate = 100f
        )

        val accepted = syncEngine.syncInboundSession(inbound)
        assertFalse(accepted)
    }

    @Test
    fun `syncInboundSession inserts session if no tombstone exists`() = runBlocking {
        val sessionUuid = "session_valid_inbound"
        val inbound = com.example.data.local.room.SessionEntity(
            uuid = sessionUuid,
            date = System.currentTimeMillis(),
            totalJumps = 350,
            detectedTotal = 350,
            correctedTotal = 350,
            activeSec = 200,
            avgRate = 105f
        )

        val accepted = syncEngine.syncInboundSession(inbound)
        assertTrue(accepted)
    }
}
