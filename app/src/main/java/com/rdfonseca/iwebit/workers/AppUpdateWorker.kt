package com.rdfonseca.iwebit.workers

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.rdfonseca.iwebit.common.Constants.APP_UPDATE_NOTIFICATION_ID
import com.rdfonseca.iwebit.common.createNotification
import com.rdfonseca.iwebit.common.createNotificationChannel
import com.rdfonseca.iwebit.common.logAndWrite
import com.rdfonseca.iwebit.common.pendingIntentFlags
import com.rdfonseca.iwebit.presentation.update.UpdateInstallActivity
import com.rdfonseca.iwebit.update.AppUpdateManager
import java.io.IOException

class AppUpdateWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        logAndWrite(applicationContext, "A verificar atualizações", "AppUpdateWorker", "doWork")
        return try {
        val manager = AppUpdateManager(applicationContext)
        val update = manager.findUpdate() ?: run {
            logAndWrite(applicationContext, "Sem atualizações disponíveis", "AppUpdateWorker", "doWork")
            return Result.success()
        }
        logAndWrite(applicationContext, "Atualização ${update.versionName} encontrada", "AppUpdateWorker", "doWork")
        val apk = manager.downloadAndValidate(update)
        createNotificationChannel(applicationContext)

        val installIntent = Intent(applicationContext, UpdateInstallActivity::class.java).apply {
            putExtra(UpdateInstallActivity.EXTRA_APK_PATH, apk.absolutePath)
            putExtra(UpdateInstallActivity.EXTRA_SHA256, update.sha256)
            putExtra(UpdateInstallActivity.EXTRA_VERSION_NAME, update.versionName)
            putExtra(UpdateInstallActivity.EXTRA_RELEASE_NOTES, update.releaseNotes)
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            update.versionCode.toInt(),
            installIntent,
            pendingIntentFlags
        )
        val installAction = NotificationCompat.Action.Builder(
            0,
            "Instalar",
            pendingIntent
        ).build()
        createNotification(
            context = applicationContext,
            title = "Atualização disponível",
            message = "iWebIT ${update.versionName} está pronta para instalar.",
            style = NotificationCompat.BigTextStyle().bigText(
                update.releaseNotes.ifBlank {
                    "iWebIT ${update.versionName} está pronta para instalar."
                }
            ),
            contentIntent = pendingIntent,
            actionNumber1 = installAction,
            notificationId = APP_UPDATE_NOTIFICATION_ID,
            cancelPrevious = true,
            autoCancel = false
        )
        logAndWrite(applicationContext, "Atualização ${update.versionName} pronta para instalar", "AppUpdateWorker", "doWork")
        Result.success()
        } catch (error: IOException) {
            logAndWrite(applicationContext, "Falha temporária na atualização: ${error.message}", "AppUpdateWorker", "doWork", "WARN")
            Result.retry()
        } catch (error: Exception) {
            logAndWrite(applicationContext, "Falha na atualização: ${error.message}", "AppUpdateWorker", "doWork", "ERROR")
            Result.failure()
        }
    }
}