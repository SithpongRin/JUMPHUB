package com.example.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SyncMetadataDao {
    @Query("SELECT value FROM sync_metadata WHERE `key` = :key LIMIT 1")
    suspend fun getValue(key: String): String?

    @Query("SELECT `key` FROM sync_metadata WHERE `key` LIKE 'tombstone_%'")
    suspend fun getAllTombstoneKeys(): List<String>

    @Query("SELECT COUNT(*) FROM sync_metadata WHERE `key` = 'tombstone_session_' || :uuid")
    suspend fun isSessionTombstoned(uuid: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setValue(entry: SyncMetadataEntity)
}
