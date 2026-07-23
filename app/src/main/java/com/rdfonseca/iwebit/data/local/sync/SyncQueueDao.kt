package com.rdfonseca.iwebit.data.local.sync

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncQueueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: PendingSyncEntity)

    @Query("SELECT * FROM pending_sync WHERE state = 'PENDING' ORDER BY createdAt ASC")
    suspend fun getPending(): List<PendingSyncEntity>

    @Query("SELECT * FROM pending_sync ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<PendingSyncEntity>>

    @Query("DELETE FROM pending_sync WHERE syncType = :syncType")
    suspend fun delete(syncType: String)

    @Query(
        """UPDATE pending_sync
           SET attemptCount = attemptCount + 1,
               lastAttemptAt = :attemptedAt
           WHERE syncType = :syncType"""
    )
    suspend fun recordAttempt(
        syncType: String,
        attemptedAt: Long
    )

    @Query(
        """UPDATE pending_sync
           SET lastError = :error,
               state = :state
           WHERE syncType = :syncType"""
    )
    suspend fun updateOutcome(
        syncType: String,
        error: String?,
        state: String
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStatus(status: SyncStatusEntity)

    @Query("SELECT * FROM sync_status WHERE id = 1")
    suspend fun getStatus(): SyncStatusEntity?

    @Query("SELECT * FROM sync_status WHERE id = 1")
    fun observeStatus(): Flow<SyncStatusEntity?>

    @Query("DELETE FROM pending_sync")
    suspend fun clearQueue()

    @Query("DELETE FROM sync_status")
    suspend fun clearStatus()
}
