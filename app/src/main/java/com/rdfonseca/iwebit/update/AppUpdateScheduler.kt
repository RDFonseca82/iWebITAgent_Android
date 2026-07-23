package com.rdfonseca.iwebit.update

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.rdfonseca.iwebit.common.Constants.WORKER_APP_UPDATE_TAG
import com.rdfonseca.iwebit.workers.AppUpdateWorker
import java.util.concurrent.TimeUnit

fun scheduleAppUpdateChecks(context: Context) {
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()
    val workManager = WorkManager.getInstance(context)

    val immediate = OneTimeWorkRequestBuilder<AppUpdateWorker>()
        .setConstraints(constraints)
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
        .build()
    workManager.enqueueUniqueWork(
        "${WORKER_APP_UPDATE_TAG}_immediate",
        ExistingWorkPolicy.KEEP,
        immediate
    )

    val periodic = PeriodicWorkRequestBuilder<AppUpdateWorker>(6, TimeUnit.HOURS)
        .setConstraints(constraints)
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
        .build()
    workManager.enqueueUniquePeriodicWork(
        WORKER_APP_UPDATE_TAG,
        ExistingPeriodicWorkPolicy.UPDATE,
        periodic
    )
}