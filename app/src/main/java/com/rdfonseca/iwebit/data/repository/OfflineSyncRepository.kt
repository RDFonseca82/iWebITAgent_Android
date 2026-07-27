package com.rdfonseca.iwebit.data.repository

import android.content.Context
import com.rdfonseca.iwebit.common.logAndWrite
import com.rdfonseca.iwebit.data.local.sync.PendingSyncEntity
import com.rdfonseca.iwebit.data.local.sync.SyncQueueDao
import com.rdfonseca.iwebit.data.local.sync.SyncStatusEntity
import com.rdfonseca.iwebit.data.remote.IWebITApi
import com.rdfonseca.iwebit.domain.model.SyncDiagnostics
import com.rdfonseca.iwebit.domain.model.SyncFlushResult
import com.rdfonseca.iwebit.domain.model.SyncType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.io.IOException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class OfflineSyncRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: IWebITApi,
    private val dao: SyncQueueDao
) {
    private val queueMutex = Mutex()

    val diagnostics: Flow<SyncDiagnostics> = combine(
        dao.observeAll(),
        dao.observeStatus()
    ) { queue, status ->
        SyncDiagnostics(
            pendingCount = queue.count { it.state == PendingSyncEntity.STATE_PENDING },
            failedCount = queue.count { it.state == PendingSyncEntity.STATE_FAILED },
            lastAttemptAt = status?.lastAttemptAt,
            lastSuccessAt = status?.lastSuccessAt,
            lastError = status?.lastError
        )
    }

    suspend fun enqueue(type: SyncType, payload: String) = queueMutex.withLock {
        logAndWrite(context, "A colocar sincronização ${type.key} na fila offline", "OfflineSyncRepository", "enqueue")
        dao.upsert(
            PendingSyncEntity(
                syncType = type.key,
                payload = payload
            )
        )
    }

    suspend fun clear() = queueMutex.withLock {
        dao.clearQueue()
        dao.clearStatus()
    }

    suspend fun flush(): SyncFlushResult = queueMutex.withLock {
        flushUnlocked()
    }

    private suspend fun flushUnlocked(): SyncFlushResult {
        var permanentFailure = false

        for (item in dao.getPending()) {
            val attemptedAt = System.currentTimeMillis()
            logAndWrite(context, "A enviar sincronização ${item.syncType} da fila offline", "OfflineSyncRepository", "flush")
            dao.recordAttempt(item.syncType, attemptedAt)
            updateStatus(lastAttemptAt = attemptedAt)

            val response = try {
                api.postSync(item.payload)
            } catch (exception: IOException) {
                val message = exception.localizedMessage ?: "Rede indisponível"
                dao.updateOutcome(item.syncType, message, PendingSyncEntity.STATE_PENDING)
                logAndWrite(context, "Sincronização ${item.syncType} adiada: $message", "OfflineSyncRepository", "flush", "WARN")
                updateStatus(lastAttemptAt = attemptedAt, error = message)
                return SyncFlushResult.RETRY
            } catch (exception: Exception) {
                val message = exception.localizedMessage ?: exception.javaClass.simpleName
                dao.updateOutcome(item.syncType, message, PendingSyncEntity.STATE_PENDING)
                logAndWrite(context, "Sincronização ${item.syncType} adiada: $message", "OfflineSyncRepository", "flush", "WARN")
                updateStatus(lastAttemptAt = attemptedAt, error = message)
                return SyncFlushResult.RETRY
            }

            if (response.isSuccessful) {
                logAndWrite(context, "Sincronização ${item.syncType} concluída", "OfflineSyncRepository", "flush")
                dao.delete(item.syncType)
                updateStatus(
                    lastAttemptAt = attemptedAt,
                    lastSuccessAt = attemptedAt,
                    clearError = true
                )
                continue
            }

            val message = buildString {
                append("HTTP ")
                append(response.code())
                response.errorBody()?.string()?.take(300)?.takeIf { it.isNotBlank() }?.let {
                    append(": ")
                    append(it)
                }
            }
            logAndWrite(context, "Sincronização ${item.syncType} rejeitada: $message", "OfflineSyncRepository", "flush", "ERROR")
            val retryable = response.code() == 408 || response.code() == 429 || response.code() >= 500
            dao.updateOutcome(
                item.syncType,
                message,
                if (retryable) PendingSyncEntity.STATE_PENDING else PendingSyncEntity.STATE_FAILED
            )
            updateStatus(lastAttemptAt = attemptedAt, error = message)

            if (retryable) return SyncFlushResult.RETRY
            permanentFailure = true
        }

        return if (permanentFailure) SyncFlushResult.FAILURE else SyncFlushResult.SUCCESS
    }

    private suspend fun updateStatus(
        lastAttemptAt: Long? = null,
        lastSuccessAt: Long? = null,
        error: String? = null,
        clearError: Boolean = false
    ) {
        val current = dao.getStatus() ?: SyncStatusEntity()
        dao.upsertStatus(
            current.copy(
                lastAttemptAt = lastAttemptAt ?: current.lastAttemptAt,
                lastSuccessAt = lastSuccessAt ?: current.lastSuccessAt,
                lastError = if (clearError) null else error ?: current.lastError
            )
        )
    }
}
