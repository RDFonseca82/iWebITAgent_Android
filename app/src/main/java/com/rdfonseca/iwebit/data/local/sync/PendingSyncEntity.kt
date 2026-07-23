package com.rdfonseca.iwebit.data.local.sync

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_sync")
data class PendingSyncEntity(
    @PrimaryKey val syncType: String,
    val payload: String,
    val createdAt: Long = System.currentTimeMillis(),
    val attemptCount: Int = 0,
    val lastAttemptAt: Long? = null,
    val lastError: String? = null,
    val state: String = STATE_PENDING
) {
    companion object {
        const val STATE_PENDING = "PENDING"
        const val STATE_FAILED = "FAILED"
    }
}
