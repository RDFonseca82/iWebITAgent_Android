package com.rdfonseca.iwebit.workers

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.rdfonseca.iwebit.common.logAndWrite
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import com.rdfonseca.iwebit.common.*
import com.rdfonseca.iwebit.data.remote.IWebITApi
import com.rdfonseca.iwebit.data.repository.UserPrefsRepositoryImpl
import com.rdfonseca.iwebit.data.util.Converters
import com.rdfonseca.iwebit.domain.model.getBodyInJsonMin
import com.rdfonseca.iwebit.services.SincLocationService

@HiltWorker
class MinSyncWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val userPrefsRepo: UserPrefsRepositoryImpl,
    private val api: IWebITApi,
    private val converters: Converters
): CoroutineWorker(context,params) {
    override suspend fun doWork(): Result {
        logAndWrite(context, "DOING MINIMAL SYNC", "AlarmSync", "makeMinSync")
        val userData = userPrefsRepo.userPreferencesFlow.first()

        if (userPrefsRepo.userPreferencesFlow.first().uniqueId.isBlank()) return Result.success()

        try {
            val body = getBodyInJsonMin(context, idsync = userData.idSync, uniqueId = userData.uniqueId)
            api.postSync(converters.toBodyJsonMin(body))

            val deviceInfo = api.getDeviceInfo(userData.uniqueId)

            deviceInfo.AndroidMessageTxt?.let{
                if (it != "null" || it.isNotBlank() || it != "0")
                    createMessageTxt(context, it)
            }

            if (deviceInfo.DeviceLocation == 1) {
                val intent = Intent(context, SincLocationService::class.java).apply {
                    action = Constants.ACTION_START_SERVICE
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }

            if (deviceInfo.AndroidPrintScreen == 1) {
                createRequestedScreenShotNotification(
                    context,
                    "Requesitada envio de captura de ecrã",
                    waitingForUserConfirmation = true,
                    capturedScreen = false
                )
            }

            val appsToUninstallJson = api.getAppsToUninstall(uniqueId = userData.uniqueId)

            if (appsToUninstallJson.body() != "null") {
                val appsToUninstall = converters
                    .fromUninstallAppsJson(appsToUninstallJson.body()!!)
                    .map { it.SoftwareName }

                context.sendBroadcast(Intent(Constants.ACTION_UPDATE_APPS_TO_UNINSTALL).apply {
                    putExtra(Constants.APPLICATIONS_DATA_EXTRA, appsToUninstall.joinToString(","))
                })

                createUninstallAppsNotification(context, appsToUninstall)
            }

            logAndWrite(context, "INFO: DEVICE LOCATION -> ${deviceInfo.DeviceLocation} | APPS TO UNINSTALL -> ${appsToUninstallJson.body()}", "AlarmSync", "makeMinSync")

            createSysTrayNotification(context, false)
        } catch(e: CancellationException) {
            return Result.success()
        } catch (e: Exception) {
            logAndWrite(context, "ERROR: ${e.localizedMessage}", "AlarmSync", "makeMinSync")
            createSysTrayNotification(context, true)
        }
        return Result.success()
    }

    private fun createMessageTxt(context: Context, message: String) {
        createNotification(
            context,
            title = "Nova Mensagem",
            message = message,
            style = NotificationCompat.BigTextStyle()
                .bigText(message),
            cancelPrevious = true,
            notificationId = Constants.REMOTE_MESSAGE_NOTIFICATION_ID
        )
    }
}
