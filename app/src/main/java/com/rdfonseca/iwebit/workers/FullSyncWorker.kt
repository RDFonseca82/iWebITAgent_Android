package com.rdfonseca.iwebit.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import com.rdfonseca.iwebit.common.Constants
import com.rdfonseca.iwebit.common.Constants.FORCE_FULL_EXTRA
import com.rdfonseca.iwebit.common.cancelNotification
import com.rdfonseca.iwebit.common.createNotification
import com.rdfonseca.iwebit.common.createSysTrayNotification
import com.rdfonseca.iwebit.common.logAndWrite
import com.rdfonseca.iwebit.data.remote.IWebITApi
import com.rdfonseca.iwebit.data.repository.UserPrefsRepositoryImpl
import com.rdfonseca.iwebit.data.util.Converters
import com.rdfonseca.iwebit.domain.model.getBodyInJsonFull

@HiltWorker
class FullSyncWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val userPrefsRepo: UserPrefsRepositoryImpl,
    private val api: IWebITApi,
    private val converters: Converters
): CoroutineWorker(context,params) {

    override suspend fun doWork(): Result {
        val forceFullSync = inputData.getBoolean(FORCE_FULL_EXTRA, false)
        val userData = userPrefsRepo.userPreferencesFlow.first()

        if (userPrefsRepo.userPreferencesFlow.first().uniqueId.isBlank()) return Result.success()

        logAndWrite(
            context,
            "DOING FULL SYNC: FORCE -> $forceFullSync ",
            "FullSyncWorker",
            "doWork"
        )

        try {
            if (forceFullSync) {
                createForceFullSyncDone(true)
            }

            api.postSync(
                converters.toBodyJsonFull(
                    getBodyInJsonFull(
                        context,
                        idsync = userData.idSync,
                        uniqueId = userData.uniqueId)
                )
            )

            if (userPrefsRepo.userPreferencesFlow.first().uniqueId.isBlank()) {
                return Result.success()
            }

            if (forceFullSync) {
                delay(500)
                userPrefsRepo.updateLastForceFullSync()
                createForceFullSyncDone()
            }

            createSysTrayNotification(context, false)
        } catch(e: CancellationException) {
            return Result.success()
        } catch (e: Exception) {
            logAndWrite(
                context,
                "ERROR ${e.javaClass}: ${e.localizedMessage}",
                "FullSyncWorker",
                "doWork"
            )
            cancelNotification(Constants.FORCE_FULL_SYNC_DONE_NOTIFICATION_ID, context)
            createSysTrayNotification(context, true)
        }
        return Result.success()
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
