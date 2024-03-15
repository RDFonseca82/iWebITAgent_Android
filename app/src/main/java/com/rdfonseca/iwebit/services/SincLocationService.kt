package com.rdfonseca.iwebit.services

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.rdfonseca.iwebit.R
import com.rdfonseca.iwebit.common.Constants.ACTION_TRY_AGAIN_LOCATION
import com.rdfonseca.iwebit.common.Constants.LOCATION_DATA_EXTRA
import com.rdfonseca.iwebit.common.Constants.LOCATION_EXCEPTION_MESSAGE
import com.rdfonseca.iwebit.common.Constants.WARNING_NOTIFICATION_ID
import com.rdfonseca.iwebit.common.Constants.WORKER_LOCATION_SYNC_TAG
import com.rdfonseca.iwebit.common.cancelNotification
import com.rdfonseca.iwebit.common.createNotification
import com.rdfonseca.iwebit.common.logAndWrite
import com.rdfonseca.iwebit.common.pendingIntentFlags
import com.rdfonseca.iwebit.presentation.MockActivityPermission
import com.rdfonseca.iwebit.presentation.TAG
import com.rdfonseca.iwebit.workers.LocationSyncWorker

class SincLocationService : Service() {

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var started = false

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (intent.action == null) {
            stopForeground(true)
            stopSelf()
            cancelNotification(WARNING_NOTIFICATION_ID, this)
            return super.onStartCommand(intent, flags, startId)
        } else {
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
            val notification = createWarningNotification(true, intent.action == ACTION_TRY_AGAIN_LOCATION)
            if (!started) {
                started = true
                startForeground(WARNING_NOTIFICATION_ID, notification)
            }
            getLastKnownLocation()
        }
        return START_STICKY
    }

    private fun getLastKnownLocation() {

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            createWarningNotification(withError = true)
            return
        }

        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var gpsEnabled = false
        var networkEnabled = false

        try {
            gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (ex: java.lang.Exception) {
        }

        try {
            networkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (ex: java.lang.Exception) {
        }
        Log.d(TAG, "getLastKnownLocation: $gpsEnabled $networkEnabled")

        if (!gpsEnabled && !networkEnabled) {
            createWarningNotification(withError = true)
            return
        }

        fusedLocationProviderClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                val lat  = location.latitude
                val lng = location.longitude
                logAndWrite(this@SincLocationService, "$lat $lng", "SincLocationService", "onLocationResult")

                val workerData = Data.Builder().apply {
                    putString(LOCATION_DATA_EXTRA, "$lat|$lng")
                }

                val oneTimeWorkRequest = OneTimeWorkRequestBuilder<LocationSyncWorker>()
                    .setInputData(workerData.build())
                    .build()

                WorkManager.getInstance(this).enqueueUniqueWork(
                    WORKER_LOCATION_SYNC_TAG,
                    ExistingWorkPolicy.REPLACE,
                    oneTimeWorkRequest
                )


                cancelNotification(WARNING_NOTIFICATION_ID, this)
                stopForeground(true)
                stopSelf()
            }
    }

    private fun createWarningNotification(withProgress: Boolean = false, withError: Boolean = false): Notification {
        val tryAgainIntent = Intent(this, MockActivityPermission::class.java).apply {
            action = ACTION_TRY_AGAIN_LOCATION
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val tryAgainPendingIntent = PendingIntent.getActivity(
            this,
            0,
            tryAgainIntent,
            pendingIntentFlags
        )

        val tryAgainAction = NotificationCompat.Action.Builder(
            0,
            "Tentar novamente",
            tryAgainPendingIntent
        ).build()

        return createNotification(
            this,
            icon = if (withError) R.drawable.ic_launcher_foreground_warning else R.drawable.ic_launcher_foreground,
            title = if (withError) "IWebIT Service - Aviso" else "IWebIT Service",
            message = if (withError) LOCATION_EXCEPTION_MESSAGE else null,
            withProgress = withProgress,
            silent = true,
            color = if (withError) 0xFFFFD900.toInt() else 0,
            style = if (withError) NotificationCompat.BigTextStyle().bigText(LOCATION_EXCEPTION_MESSAGE) else null,
            actionNumber1 = if (withError) tryAgainAction else null,
            notificationId = WARNING_NOTIFICATION_ID
        )
    }

    override fun onBind(p0: Intent?): IBinder? = null
}