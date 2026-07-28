package com.rdfonseca.iwebit.common

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.rdfonseca.iwebit.R
import com.rdfonseca.iwebit.common.Constants.ACTION_REQUESTED_ASK_AND_CAPTURE_SCREEN
import com.rdfonseca.iwebit.common.Constants.APPLICATIONS_DATA_EXTRA
import com.rdfonseca.iwebit.common.Constants.CHANNEL_ID
import com.rdfonseca.iwebit.common.Constants.CHANNEL_NAME
import com.rdfonseca.iwebit.common.Constants.FORCE_FULL_EXTRA
import com.rdfonseca.iwebit.common.Constants.FORCE_FULL_SYNC_DONE_NOTIFICATION_ID
import com.rdfonseca.iwebit.common.Constants.REMOTE_MESSAGE_NOTIFICATION_ID
import com.rdfonseca.iwebit.common.Constants.SCREENSHOT_NOTIFICATION_ID
import com.rdfonseca.iwebit.common.Constants.SCREENSHOT_REQUESTED_DONE_NOTIFICATION_ID
import com.rdfonseca.iwebit.common.Constants.SCREENSHOT_REQUESTED_NOTIFICATION_ID
import com.rdfonseca.iwebit.common.Constants.SYSTRAY_NOTIFICATION_ID
import com.rdfonseca.iwebit.common.Constants.UNINSTALL_APPS_NOTIFICATION_ID
import com.rdfonseca.iwebit.common.Constants.WARNING_NOTIFICATION_ID
import com.rdfonseca.iwebit.common.Constants.WORKER_FORCE_SYNC_TAG
import com.rdfonseca.iwebit.common.Constants.WORKER_FULL_SYNC_TAG
import com.rdfonseca.iwebit.common.Constants.WORKER_LOCATION_SYNC_TAG
import com.rdfonseca.iwebit.common.Constants.WORKER_MIN_SYNC_TAG
import com.rdfonseca.iwebit.common.Constants.WORKER_INITIAL_SYNC_TAG
import com.rdfonseca.iwebit.presentation.MainActivity
import com.rdfonseca.iwebit.presentation.MockActivityPermission
import com.rdfonseca.iwebit.presentation.UninstallAppsActivity
import com.rdfonseca.iwebit.workers.FullSyncWorker
import com.rdfonseca.iwebit.workers.MinSyncWorker
import java.util.concurrent.TimeUnit

val pendingIntentFlags =
    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

val syncNetworkConstraints: Constraints = Constraints.Builder()
    .setRequiredNetworkType(NetworkType.CONNECTED)
    .build()

const val SYNC_BACKOFF_SECONDS = 30L

fun setupFullSyncWorker(
    context: Context
) {
    val workerData = Data.Builder().apply {
        putBoolean(FORCE_FULL_EXTRA, false)
    }

    val periodicWorkRequest = PeriodicWorkRequestBuilder<FullSyncWorker>(
        1,
        TimeUnit.HOURS
    )
        .setInputData(workerData.build())
        .setConstraints(syncNetworkConstraints)
        .setBackoffCriteria(
            BackoffPolicy.EXPONENTIAL,
            SYNC_BACKOFF_SECONDS,
            TimeUnit.SECONDS
        )
        .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        WORKER_FULL_SYNC_TAG,
        ExistingPeriodicWorkPolicy.UPDATE,
        periodicWorkRequest
    )
}

fun setupMinSyncWorker(
    context: Context
) {
    val periodicWorkRequest = PeriodicWorkRequestBuilder<MinSyncWorker>(
        15,
        TimeUnit.MINUTES
    )
        .setConstraints(syncNetworkConstraints)
        .setBackoffCriteria(
            BackoffPolicy.EXPONENTIAL,
            SYNC_BACKOFF_SECONDS,
            TimeUnit.SECONDS
        )
        .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        WORKER_MIN_SYNC_TAG,
        ExistingPeriodicWorkPolicy.UPDATE,
        periodicWorkRequest
    )
}

fun enqueueInitialSync(context: Context) {
    val minimalSync = OneTimeWorkRequestBuilder<MinSyncWorker>()
        .setConstraints(syncNetworkConstraints)
        .setBackoffCriteria(
            BackoffPolicy.EXPONENTIAL,
            SYNC_BACKOFF_SECONDS,
            TimeUnit.SECONDS
        )
        .build()

    val fullSync = OneTimeWorkRequestBuilder<FullSyncWorker>()
        .setInputData(
            Data.Builder()
                .putBoolean(FORCE_FULL_EXTRA, false)
                .build()
        )
        .setConstraints(syncNetworkConstraints)
        .setBackoffCriteria(
            BackoffPolicy.EXPONENTIAL,
            SYNC_BACKOFF_SECONDS,
            TimeUnit.SECONDS
        )
        .build()

    WorkManager.getInstance(context)
        .beginUniqueWork(
            WORKER_INITIAL_SYNC_TAG,
            ExistingWorkPolicy.KEEP,
            minimalSync
        )
        .then(fullSync)
        .enqueue()
}

fun createNotificationChannel(context: Context) {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    val description = "Channel for System Tray Icon"
    val importance = NotificationManager.IMPORTANCE_HIGH
    val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance)
    channel.description = description

    manager.createNotificationChannel(channel)
}

fun createNotification(
    context: Context,
    icon: Int = R.drawable.ic_launcher_foreground,
    title: String = "",
    message: String? = null,
    withProgress: Boolean = false,
    autoCancel: Boolean = true,
    persistent: Boolean = false,
    showTime: Boolean = true,
    category: String? = null,
    silent: Boolean = false,
    style: NotificationCompat.Style? = null,
    color: Int = 0,
    contentIntent: PendingIntent? = null,
    actionNumber1: NotificationCompat.Action? = null,
    actionNumber2: NotificationCompat.Action? = null,
    notificationId: Int,
    channelId: String = CHANNEL_ID,
    cancelPrevious: Boolean = false
): Notification {
    if (cancelPrevious) {
        cancelNotification(notificationId, context)
    }

    val builder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(icon)
        .setContentTitle(title)
        .setContentText(message)
        .setProgress(0, 0, withProgress)
        .setAutoCancel(autoCancel)
        .setOngoing(persistent)
        .setShowWhen(showTime)
        .setCategory(category)
        .setSilent(silent)
        .setColor(color)
        .setStyle(style)
        .addAction(actionNumber1)
        .addAction(actionNumber2)
        .setContentIntent(contentIntent)
        .build()

    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    manager.notify(notificationId, builder)

    return builder

}


fun createSysTrayNotification(context: Context, requestFailed: Boolean = false) {
    val activityIntent = Intent(context, MainActivity::class.java).apply {
        action = Intent.ACTION_MAIN
        addCategory(Intent.CATEGORY_LAUNCHER)
    }
    val contentIntent = PendingIntent.getActivity(
        context,
        0,
        activityIntent,
        pendingIntentFlags
    )

    val screenShotIntent = Intent(context, MockActivityPermission::class.java).apply {
        action = Constants.ACTION_ASK_AND_CAPTURE_SCREEN
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val screenShotPendingIntent = PendingIntent.getActivity(
        context,
        0,
        screenShotIntent,
        pendingIntentFlags
    )

    val forceFullSyncIntent = Intent(context, MockActivityPermission::class.java).apply {
        action = Constants.ACTION_FORCE_FULL_SYNC
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val forceFullSyncPendingIntent = PendingIntent.getActivity(
        context,
        0,
        forceFullSyncIntent,
        pendingIntentFlags
    )

    val forceSync = NotificationCompat.Action.Builder(
        0,
        "Forçar Sincronização",
        forceFullSyncPendingIntent
    ).build()
    val sendScreenshot = NotificationCompat.Action.Builder(
        0,
        "Capturar ecrã e enviar",
        screenShotPendingIntent
    ).build()

    createNotification(
        context = context,
        icon = if (!requestFailed) R.drawable.ic_launcher_foreground_on else R.drawable.ic_launcher_foreground_off,
        title = if (!requestFailed) "IWebIT Service" else "IWebIT Service - OFFLINE",
        autoCancel = false,
        persistent = true,
        showTime = false,
        silent = true,
        color = if (!requestFailed) 0xFF00FF00.toInt() else 0xFFFF0000.toInt(),
        contentIntent = contentIntent,
        actionNumber1 = forceSync,
        actionNumber2 = sendScreenshot,
        notificationId = SYSTRAY_NOTIFICATION_ID
    )
}

fun createUninstallAppsNotification(context: Context, apps: List<String>, silent: Boolean = false) {
    val seeAppsIntent = Intent(context, UninstallAppsActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        putExtra(APPLICATIONS_DATA_EXTRA, apps.joinToString(","))
    }
    val seeAppsPendingIntent = PendingIntent.getActivity(
        context,
        0,
        seeAppsIntent,
        pendingIntentFlags
    )
    val seeAppsAction = NotificationCompat.Action.Builder(
        0,
        "Ver",
        seeAppsPendingIntent
    ).build()

    val numberOfApps = apps.size


    val message = if (numberOfApps == 1) {
        "O gerenciador aconselha a desinstalação desta aplicação."
    } else {
        "O gerenciador aconselha a desinstalação destas $numberOfApps aplicações."
    }
    val title = if (numberOfApps == 1) {
        "Desinstale esta app"
    } else {
        "Desinstale estas apps"
    }

    createNotification(
        context,
        title = title,
        message = message,
        autoCancel = false,
        persistent = true,
        cancelPrevious = true,
        silent = silent,
        contentIntent = seeAppsPendingIntent,
        style = NotificationCompat.BigTextStyle()
            .bigText(message),
        actionNumber1 = seeAppsAction,
        notificationId = UNINSTALL_APPS_NOTIFICATION_ID
    )
}


fun createRequestedScreenShotNotification(
    context: Context,
    message: String = "",
    waitingForUserConfirmation: Boolean,
    capturedScreen: Boolean
): Notification {

    val takeScreenShotIntent = Intent(context, MockActivityPermission::class.java).apply {
        action = ACTION_REQUESTED_ASK_AND_CAPTURE_SCREEN
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val takeScreenShotPendingIntent = PendingIntent.getActivity(
        context,
        0,
        takeScreenShotIntent,
        pendingIntentFlags
    )
    val actionTakeScreenShot = if (waitingForUserConfirmation) NotificationCompat.Action.Builder(
        0,
        "Enviar captura de ecrã",
        takeScreenShotPendingIntent
    ).build() else null

    return createNotification(
        context = context,
        title = "IWebIT",
        message = message,
        notificationId = SCREENSHOT_REQUESTED_NOTIFICATION_ID,
        cancelPrevious = true,
        persistent = !capturedScreen,
        autoCancel = false,
        actionNumber1 = actionTakeScreenShot
    )
}

fun cancelNotification(
    notificationId: Int,
    context: Context
) {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    manager.cancel(notificationId)

}

fun cancelAllNotifications(context: Context) {
    val manager = WorkManager.getInstance(context)

    manager.cancelUniqueWork(WORKER_FULL_SYNC_TAG)
    manager.cancelUniqueWork(WORKER_MIN_SYNC_TAG)
    manager.cancelUniqueWork(WORKER_FORCE_SYNC_TAG)
    manager.cancelUniqueWork(WORKER_INITIAL_SYNC_TAG)
    manager.cancelUniqueWork(WORKER_LOCATION_SYNC_TAG)

    cancelNotification(SYSTRAY_NOTIFICATION_ID, context)
    cancelNotification(SCREENSHOT_NOTIFICATION_ID, context)
    cancelNotification(SCREENSHOT_REQUESTED_NOTIFICATION_ID, context)
    cancelNotification(SCREENSHOT_REQUESTED_DONE_NOTIFICATION_ID, context)
    cancelNotification(REMOTE_MESSAGE_NOTIFICATION_ID, context)
    cancelNotification(FORCE_FULL_SYNC_DONE_NOTIFICATION_ID, context)
    cancelNotification(UNINSTALL_APPS_NOTIFICATION_ID, context)
    cancelNotification(WARNING_NOTIFICATION_ID, context)
}

