package com.rdfonseca.iwebit.receivers

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import androidx.core.app.NotificationCompat
import com.rdfonseca.iwebit.common.Constants.APP_UPDATE_NOTIFICATION_ID
import com.rdfonseca.iwebit.common.cancelNotification
import com.rdfonseca.iwebit.common.createNotification
import com.rdfonseca.iwebit.common.createNotificationChannel
import com.rdfonseca.iwebit.common.logAndWrite
import com.rdfonseca.iwebit.common.pendingIntentFlags
import com.rdfonseca.iwebit.presentation.update.UpdateInstallActivity

class AppUpdateStatusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_PACKAGE_UPDATE_STATUS) return

        when (val status = intent.getIntExtra(
            PackageInstaller.EXTRA_STATUS,
            PackageInstaller.STATUS_FAILURE
        )) {
            PackageInstaller.STATUS_SUCCESS -> {
                cancelNotification(APP_UPDATE_NOTIFICATION_ID, context)
                logAndWrite(context, "Atualização instalada com sucesso", TAG, "onReceive")
            }

            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirmation = confirmationIntent(intent)
                if (confirmation == null) {
                    showRetryNotification(context, intent, "Confirmação do Android indisponível")
                    return
                }
                confirmation.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                val pendingConfirmation = PendingIntent.getActivity(
                    context,
                    requestCode(intent),
                    confirmation,
                    pendingIntentFlags
                )
                createNotificationChannel(context)
                createNotification(
                    context = context,
                    title = "Confirmação necessária",
                    message = "Toque para concluir a atualização iWebIT.",
                    contentIntent = pendingConfirmation,
                    actionNumber1 = NotificationCompat.Action.Builder(
                        0,
                        "Instalar",
                        pendingConfirmation
                    ).build(),
                    notificationId = APP_UPDATE_NOTIFICATION_ID,
                    cancelPrevious = true,
                    autoCancel = false
                )
                logAndWrite(
                    context,
                    "Android exigiu confirmação para concluir a atualização",
                    TAG,
                    "onReceive",
                    "WARN"
                )
            }

            else -> {
                val detail = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                    ?.takeIf { it.isNotBlank() }
                    ?: "estado $status"
                showRetryNotification(context, intent, detail)
                logAndWrite(
                    context,
                    "Falha na instalação da atualização: $detail",
                    TAG,
                    "onReceive",
                    "ERROR"
                )
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun confirmationIntent(intent: Intent): Intent? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
        } else {
            intent.getParcelableExtra(Intent.EXTRA_INTENT)
        }

    private fun showRetryNotification(context: Context, statusIntent: Intent, detail: String) {
        val retryIntent = UpdateInstallActivity.createIntent(
            context = context,
            apkPath = statusIntent.getStringExtra(EXTRA_APK_PATH).orEmpty(),
            sha256 = statusIntent.getStringExtra(EXTRA_SHA256).orEmpty(),
            versionCode = statusIntent.getLongExtra(EXTRA_VERSION_CODE, 0),
            versionName = statusIntent.getStringExtra(EXTRA_VERSION_NAME).orEmpty(),
            releaseNotes = statusIntent.getStringExtra(EXTRA_RELEASE_NOTES).orEmpty()
        )
        val retry = PendingIntent.getActivity(
            context,
            requestCode(statusIntent),
            retryIntent,
            pendingIntentFlags
        )
        createNotificationChannel(context)
        createNotification(
            context = context,
            title = "Atualização requer atenção",
            message = detail,
            contentIntent = retry,
            actionNumber1 = NotificationCompat.Action.Builder(0, "Tentar novamente", retry).build(),
            notificationId = APP_UPDATE_NOTIFICATION_ID,
            cancelPrevious = true,
            autoCancel = false
        )
    }

    private fun requestCode(intent: Intent): Int =
        intent.getLongExtra(EXTRA_VERSION_CODE, 0).hashCode()

    companion object {
        const val ACTION_PACKAGE_UPDATE_STATUS =
            "com.rdfonseca.iwebit.action.PACKAGE_UPDATE_STATUS"
        const val EXTRA_APK_PATH = "update_apk_path"
        const val EXTRA_SHA256 = "update_sha256"
        const val EXTRA_VERSION_CODE = "update_version_code"
        const val EXTRA_VERSION_NAME = "update_version_name"
        const val EXTRA_RELEASE_NOTES = "update_release_notes"
        private const val TAG = "AppUpdateStatusReceiver"
    }
}
