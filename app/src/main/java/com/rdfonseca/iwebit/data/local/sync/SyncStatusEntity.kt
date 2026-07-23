package com.rdfonseca.iwebit.data.local.sync

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_status")
data class SyncStatusEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val lastAttemptAt: Long? = null,
    val lastSuccessAt: Long? = null,
    val lastError: String? = null
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
