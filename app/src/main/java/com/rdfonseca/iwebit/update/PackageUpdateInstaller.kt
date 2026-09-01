package com.rdfonseca.iwebit.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.os.Build
import com.rdfonseca.iwebit.domain.model.AppUpdate
import com.rdfonseca.iwebit.receivers.AppUpdateStatusReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class PackageUpdateInstaller(private val context: Context) {
    fun canRequestPackageInstalls(): Boolean =
        context.packageManager.canRequestPackageInstalls()

    suspend fun submit(apk: File, update: AppUpdate): PackageInstallSubmission =
        withContext(Dispatchers.IO) {
            if (!canRequestPackageInstalls()) {
                return@withContext PackageInstallSubmission.PermissionRequired
            }

            val packageInstaller = context.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL
            ).apply {
                setAppPackageName(context.packageName)
                setSize(apk.length())
                setInstallReason(PackageManager.INSTALL_REASON_USER)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    setPackageSource(PackageInstaller.PACKAGE_SOURCE_DOWNLOADED_FILE)
                }
            }
            val sessionId = packageInstaller.createSession(params)
            var committed = false

            try {
                packageInstaller.openSession(sessionId).use { session ->
                    session.openWrite(APK_SESSION_NAME, 0, apk.length()).use { output ->
                        apk.inputStream().use { input -> input.copyTo(output) }
                        session.fsync(output)
                    }

                    val callbackIntent = Intent(context, AppUpdateStatusReceiver::class.java).apply {
                        action = AppUpdateStatusReceiver.ACTION_PACKAGE_UPDATE_STATUS
                        putExtra(AppUpdateStatusReceiver.EXTRA_APK_PATH, apk.absolutePath)
                        putExtra(AppUpdateStatusReceiver.EXTRA_SHA256, update.sha256)
                        putExtra(AppUpdateStatusReceiver.EXTRA_VERSION_CODE, update.versionCode)
                        putExtra(AppUpdateStatusReceiver.EXTRA_VERSION_NAME, update.versionName)
                        putExtra(AppUpdateStatusReceiver.EXTRA_RELEASE_NOTES, update.releaseNotes)
                    }
                    val callback = PendingIntent.getBroadcast(
                        context,
                        sessionId,
                        callbackIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                PendingIntent.FLAG_MUTABLE
                            } else {
                                0
                            }
                    )
                    session.commit(callback.intentSender)
                    committed = true
                }
            } finally {
                if (!committed) packageInstaller.abandonSession(sessionId)
            }

            PackageInstallSubmission.Submitted(
                silentRequested = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            )
        }

    companion object {
        private const val APK_SESSION_NAME = "base.apk"
    }
}

sealed interface PackageInstallSubmission {
    data object PermissionRequired : PackageInstallSubmission
    data class Submitted(val silentRequested: Boolean) : PackageInstallSubmission
}
