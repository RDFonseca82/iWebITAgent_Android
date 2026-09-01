package com.rdfonseca.iwebit.workers

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
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
import com.rdfonseca.iwebit.background.BackgroundUpdateResult
import com.rdfonseca.iwebit.background.BackgroundWallpaperManager
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
    private val converters: Converters,
    private val backgroundWallpaperManager: BackgroundWallpaperManager
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        logAndWrite(context, "DOING MINIMAL SYNC", "MinSyncWorker", "doWork")
        val userData = userPrefsRepo.userPreferencesFlow.first()
        if (userData.uniqueId.isBlank() || userData.idSync.isBlank()) {
            logAndWrite(context, "Sincronização ignorada: identidade ou IDSync em falta", "MinSyncWorker", "doWork", "WARN")
            return Result.success()
        }

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

            try {
                when (
                    val backgroundResult = backgroundWallpaperManager.synchronize(
                        setBackground = deviceInfo.SetBackground,
                        backgroundImage = deviceInfo.BackgroundImage
                    )
                ) {
                    is BackgroundUpdateResult.Applied -> logAndWrite(
                        context,
                        "Background aplicado a partir de ${backgroundResult.sourceHost}",
                        "MinSyncWorker",
                        "doWork"
                    )
                    BackgroundUpdateResult.AlreadyApplied,
                    BackgroundUpdateResult.NoRequest -> Unit
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                logAndWrite(
                    context,
                    "Falha ao aplicar background: ${exception.localizedMessage}",
                    "MinSyncWorker",
                    "doWork",
                    "ERROR"
                )
            }

            if (deviceInfo.DeviceLocation == 1) {
                val hasLocationPermission =
                    ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED

                val hasBackgroundLocationPermission =
                    android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q ||
                        ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED

                if (hasLocationPermission && hasBackgroundLocationPermission) {
                    val intent = Intent(context, SincLocationService::class.java).apply {
                        action = Constants.ACTION_START_SERVICE
                    }
                    context.startForegroundService(intent)
                } else {
                    logAndWrite(
                        context,
                        "Pedido remoto de localização adiado: permissão indisponível",
                        "MinSyncWorker",
                        "doWork",
                        "WARN"
                    )
                }
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
