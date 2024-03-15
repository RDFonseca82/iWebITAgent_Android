package com.rdfonseca.iwebit.common

import android.content.Context
import android.content.IntentSender
import androidx.activity.result.IntentSenderRequest
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task

fun checkLocationSetting(
    context: Context,
    onDisabled: (IntentSenderRequest?) -> Unit
) {
    val locationRequest = LocationRequest.create().apply {
        interval = 1000
        fastestInterval = 1000
        priority = Priority.PRIORITY_HIGH_ACCURACY
    }

    val client: SettingsClient = LocationServices.getSettingsClient(context)
    val builder: LocationSettingsRequest.Builder = LocationSettingsRequest
        .Builder()
        .addLocationRequest(locationRequest)

    val gpsSettingTask: Task<LocationSettingsResponse> =
        client.checkLocationSettings(builder.build())

    gpsSettingTask.addOnFailureListener { exception ->
        var intentSenderRequest: IntentSenderRequest? = null
        if (exception is ResolvableApiException) {
            try {
                intentSenderRequest = IntentSenderRequest
                    .Builder(exception.resolution)
                    .build()
            } catch (sendEx: IntentSender.SendIntentException) {

            }
        }
        onDisabled(intentSenderRequest)
    }

}