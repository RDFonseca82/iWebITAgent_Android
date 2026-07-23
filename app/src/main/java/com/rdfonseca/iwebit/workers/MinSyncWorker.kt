package com.rdfonseca.iwebit.workers

import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.rdfonseca.iwebit.common.Constants
import com.rdfonseca.iwebit.common.createNotification
import com.rdfonseca.iwebit.common.createRequestedScreenShotNotification
import com.rdfonseca.iwebit.common.createSysTrayNotification
import com.rdfonseca.iwebit.common.createUninstallAppsNotification
import com.rdfonseca.iwebit.common.logAndWrite
import com.rdfonseca.iwebit.data.remote.IWebITApi
import com.rdfonseca.iwebit.data.repository.OfflineSyncRepository
import com.rdfonseca.iwebit.data.repository.UserPrefsRepositoryImpl
import com.rdfonseca.iwebit.data.util.Converters
import com.rdfonseca.iwebit.domain.model.SyncFlushResult
import com.rdfonseca.iwebit.domain.model.SyncType
import com.rdfonseca.iwebit.domain.model.getBodyInJsonMin
import com.rdfonseca.iwebit.services.SincLocationService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.io.IOException

@HiltWorker
class MinSyncWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val userPrefsRepo: UserPrefsRepositoryImpl,
    private val api: IWebITApi,
    private val offlineSyncRepository: OfflineSyncRepository,
    private val converters: Converters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        logAndWrite(context, "DOING MINIMAL SYNC", "MinSyncWorker", "doWork")
        val userData = userPrefsRepo.userPreferencesFlow.first()
        if (userData.uniqueId.isBlank()) return Result.success()

        return try {
            val payload = converters.toBodyJsonMin(
                getBodyInJsonMin(context, idsync = userData.idSync, uniqueId = userData.uniqueId)
            )
            offlineSyncRepository.enqueue(SyncType.MINIMAL, payload)

            when (offlineSyncRepository.flush()) {
                SyncFlushResult.RETRY -> return Result.retry()
                SyncFlushResult.FAILURE -> return Result.failure()
                SyncFlushResult.SUCCESS -> Unit
            }

            val deviceInfo = api.getDeviceInfo(userData.uniqueId)
            deviceInfo.MessageTxt?.takeIf { it != "null" && it.isNotBlank() && it != "0" }?.let {
                createMessageTxt(context, it)
            }

            if (deviceInfo.DeviceLocation == 1) {
                val intent = Intent(context, SincLocationService::class.java).apply {
                    action = Constants.ACTION_START_SERVICE
                }
                context.startForegroundService(intent)
            }

            if (deviceInfo.AndroidPrintScreen == 1) {
                createRequestedScreenShotNotification(
                    context,
                    "Requisitado envio de captura de ecrã",
                    waitingForUserConfirmation = true,
                    capturedScreen = false
                )
            }

            if (deviceInfo.SupportBundle == 1) {
                val supportBundleRequest = OneTimeWorkRequestBuilder<FullSyncWorker>()
                    .setInputData(
                        workDataOf(
                            Constants.FORCE_FULL_EXTRA to false,
                            Constants.SUPPORT_BUNDLE_EXTRA to true
                        )
                    )
                    .build()
                WorkManager.getInstance(context).enqueueUniqueWork(
                    Constants.WORKER_SUPPORT_BUNDLE_TAG,
                    ExistingWorkPolicy.KEEP,
                    supportBundleRequest
                )
            }

            val appsResponse = api.getAppsToUninstall(uniqueId = userData.uniqueId)
            if (!appsResponse.isSuccessful) throw IOException("HTTP ${appsResponse.code()}")
            val appsJson = appsResponse.body()
            if (!appsJson.isNullOrBlank() && appsJson != "null") {
                val appsToUninstall = converters.fromUninstallAppsJson(appsJson).map { it.SoftwareName }
                context.sendBroadcast(Intent(Constants.ACTION_UPDATE_APPS_TO_UNINSTALL).apply {
                    setPackage(context.packageName)
                    putExtra(Constants.APPLICATIONS_DATA_EXTRA, appsToUninstall.joinToString(","))
                })
                createUninstallAppsNotification(context, appsToUninstall)
            }

            logAndWrite(
                context,
                "INFO: DEVICE LOCATION -> ${deviceInfo.DeviceLocation} | APPS TO UNINSTALL -> $appsJson",
                "MinSyncWorker",
                "doWork"
            )
            createSysTrayNotification(context, false)
            Result.success()
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            logAndWrite(context, "ERROR: ${exception.localizedMessage}", "MinSyncWorker", "doWork")
            createSysTrayNotification(context, true)
            Result.retry()
        }
    }

    private fun createMessageTxt(context: Context, message: String) {
        createNotification(
            context,
            title = "Nova Mensagem",
            message = message,
            style = NotificationCompat.BigTextStyle().bigText(message),
            cancelPrevious = true,
            notificationId = Constants.REMOTE_MESSAGE_NOTIFICATION_ID
        )
    }
}
