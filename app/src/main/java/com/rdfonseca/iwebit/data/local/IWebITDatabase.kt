package com.rdfonseca.iwebit.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.rdfonseca.iwebit.data.local.sync.PendingSyncEntity
import com.rdfonseca.iwebit.data.local.sync.SyncQueueDao
import com.rdfonseca.iwebit.data.local.sync.SyncStatusEntity

@Database(
    entities = [PendingSyncEntity::class, SyncStatusEntity::class],
    version = 1,
    exportSchema = false
)
abstract class IWebITDatabase : RoomDatabase() {
    abstract fun syncQueueDao(): SyncQueueDao
}
