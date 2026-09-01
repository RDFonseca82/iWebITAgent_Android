package com.rdfonseca.iwebit.workers

import android.app.PendingIntent
import android.content.Context
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
import com.rdfonseca.iwebit.update.PackageInstallSubmission
import com.rdfonseca.iwebit.update.PackageUpdateInstaller
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
                logAndWrite(
                    applicationContext,
                    "Sem atualizações disponíveis",
                    "AppUpdateWorker",
                    "doWork"
                )
                return Result.success()
            }
            logAndWrite(
                applicationContext,
                "Atualização ${update.versionName} encontrada",
                "AppUpdateWorker",
                "doWork"
            )
            val apk = manager.downloadAndValidate(update)
            createNotificationChannel(applicationContext)

            when (val submission = PackageUpdateInstaller(applicationContext).submit(apk, update)) {
                is PackageInstallSubmission.Submitted -> {
                    logAndWrite(
                        applicationContext,
                        "Atualização ${update.versionName} enviada ao PackageInstaller; " +
                            "silenciosa=${submission.silentRequested}",
                        "AppUpdateWorker",
                        "doWork"
                    )
                    return Result.success()
                }
                PackageInstallSubmission.PermissionRequired -> Unit
            }

            val installIntent = UpdateInstallActivity.createIntent(
                context = applicationContext,
                apkPath = apk.absolutePath,
                sha256 = update.sha256,
                versionCode = update.versionCode,
                versionName = update.versionName,
                releaseNotes = update.releaseNotes
            )
            val pendingIntent = PendingIntent.getActivity(
                applicationContext,
                update.versionCode.hashCode(),
                installIntent,
                pendingIntentFlags
            )
            val installAction = NotificationCompat.Action.Builder(
                0,
                "Autorizar",
                pendingIntent
            ).build()
            createNotification(
                context = applicationContext,
                title = "Autorização necessária",
                message = "Autorize o iWebIT a instalar a atualização ${update.versionName}.",
                style = NotificationCompat.BigTextStyle().bigText(
                    update.releaseNotes.ifBlank {
                        "É necessária uma autorização inicial do Android para instalar atualizações."
                    }
                ),
                contentIntent = pendingIntent,
                actionNumber1 = installAction,
                notificationId = APP_UPDATE_NOTIFICATION_ID,
                cancelPrevious = true,
                autoCancel = false
            )
            logAndWrite(
                applicationContext,
                "Atualização ${update.versionName} aguarda autorização para instalar APKs",
                "AppUpdateWorker",
                "doWork",
                "WARN"
            )
            Result.success()
        } catch (error: IOException) {
            logAndWrite(
                applicationContext,
                "Falha temporária na atualização: ${error.message}",
                "AppUpdateWorker",
                "doWork",
                "WARN"
            )
            Result.retry()
        } catch (error: Exception) {
            logAndWrite(
                applicationContext,
                "Falha na atualização: ${error.message}",
                "AppUpdateWorker",
                "doWork",
                "ERROR"
            )
            Result.failure()
        }
    }
}
