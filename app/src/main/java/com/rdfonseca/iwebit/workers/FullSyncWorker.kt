package com.rdfonseca.iwebit.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.rdfonseca.iwebit.common.Constants
import com.rdfonseca.iwebit.common.Constants.FORCE_FULL_EXTRA
import com.rdfonseca.iwebit.common.cancelNotification
import com.rdfonseca.iwebit.common.createNotification
import com.rdfonseca.iwebit.common.createSysTrayNotification
import com.rdfonseca.iwebit.common.logAndWrite
import com.rdfonseca.iwebit.data.repository.OfflineSyncRepository
import com.rdfonseca.iwebit.data.repository.UserPrefsRepositoryImpl
import com.rdfonseca.iwebit.data.util.Converters
import com.rdfonseca.iwebit.domain.model.SyncFlushResult
import com.rdfonseca.iwebit.domain.model.SyncType
import com.rdfonseca.iwebit.domain.model.getBodyInJsonFull
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

class FullSyncWorker(
    private val context: Context,
    params: WorkerParameters,
    private val userPrefsRepo: UserPrefsRepositoryImpl,
    private val offlineSyncRepository: OfflineSyncRepository,
    private val converters: Converters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val forceFullSync = inputData.getBoolean(FORCE_FULL_EXTRA, false)
        val includeSupportBundle = inputData.getBoolean(Constants.SUPPORT_BUNDLE_EXTRA, false)
        val userData = userPrefsRepo.userPreferencesFlow.first()
        if (userData.uniqueId.isBlank() || userData.idSync.isBlank()) {
            logAndWrite(context, "Sincronização ignorada: identidade ou IDSync em falta", "FullSyncWorker", "doWork", "WARN")
            return Result.success()
        }

        logAndWrite(
            context,
            "DOING FULL SYNC: FORCE -> $forceFullSync | SUPPORT BUNDLE -> $includeSupportBundle",
            "FullSyncWorker",
            "doWork"
        )

        return try {
            if (forceFullSync) createForceFullSyncDone(true)

            val syncDiagnostics = offlineSyncRepository.diagnostics.first()
            val payload = converters.toBodyJsonFull(
                getBodyInJsonFull(
                    context,
                    idsync = userData.idSync,
                    uniqueId = userData.uniqueId,
                    syncDiagnostics = syncDiagnostics,
                    includeSupportBundle = includeSupportBundle,
                    supportBundleReason = "REMOTE_REQUEST"
                )
            )
            offlineSyncRepository.enqueue(SyncType.FULL, payload)

            when (offlineSyncRepository.flush()) {
                SyncFlushResult.SUCCESS -> {
                    if (forceFullSync) {
                        delay(500)
                        userPrefsRepo.updateLastForceFullSync()
                        createForceFullSyncDone()
                    }
                    createSysTrayNotification(context, false)
                    Result.success()
                }

                SyncFlushResult.RETRY -> {
                    cancelNotification(Constants.FORCE_FULL_SYNC_DONE_NOTIFICATION_ID, context)
                    createSysTrayNotification(context, true)
                    Result.retry()
                }

                SyncFlushResult.FAILURE -> {
                    cancelNotification(Constants.FORCE_FULL_SYNC_DONE_NOTIFICATION_ID, context)
                    createSysTrayNotification(context, true)
                    Result.failure()
                }
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            logAndWrite(
                context,
                "ERROR ${exception.javaClass}: ${exception.localizedMessage}",
                "FullSyncWorker",
                "doWork"
            )
            cancelNotification(Constants.FORCE_FULL_SYNC_DONE_NOTIFICATION_ID, context)
            createSysTrayNotification(context, true)
            Result.retry()
        }
    }

    private fun createForceFullSyncDone(withProgress: Boolean = false) {
        createNotification(
            context,
            title = if (withProgress) "A Sincronizar..." else "Sincronização realizada com sucesso",
            withProgress = withProgress,
            autoCancel = !withProgress,
            persistent = withProgress,
            cancelPrevious = true,
            notificationId = Constants.FORCE_FULL_SYNC_DONE_NOTIFICATION_ID
        )
    }
}
