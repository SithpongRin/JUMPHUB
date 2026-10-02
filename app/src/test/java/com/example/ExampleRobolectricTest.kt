package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.room.AppDatabase
import com.example.data.local.room.SessionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("JUMPHUB", appName)
    }

    @Test
    fun `database initialization and session insertion`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getInstance(context)
        assertNotNull(db)

        val session = SessionEntity(
            uuid = UUID.randomUUID().toString(),
            date = System.currentTimeMillis(),
            totalJumps = 150,
            detectedTotal = 150,
            correctedTotal = 150,
            activeSec = 120,
            avgRate = 75f
        )
        db.sessionDao().insertSession(session)

        val retrieved = db.sessionDao().getAllSessions().first()
        assertEquals(1, retrieved.size)
        assertEquals(150, retrieved[0].totalJumps)
    }
}
