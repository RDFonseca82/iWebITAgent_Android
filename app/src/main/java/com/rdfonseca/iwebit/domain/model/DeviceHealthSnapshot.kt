package com.rdfonseca.iwebit.domain.model

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.StatFs
import android.os.SystemClock
import androidx.annotation.Keep
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.rdfonseca.iwebit.BuildConfig

@Keep
data class DeviceHealthSnapshot(
    val collectedAt: Long,
    val batteryLevelPercent: Int,
    val isCharging: Boolean,
    val batteryTemperatureCelsius: Double?,
    val availableStorageBytes: Long,
    val totalStorageBytes: Long,
    val availableMemoryBytes: Long,
    val totalMemoryBytes: Long,
    val lowMemory: Boolean,
    val networkType: String,
    val networkValidated: Boolean,
    val deviceUptimeMillis: Long,
    val locationEnabled: Boolean,
    val foregroundLocationPermission: Boolean,
    val backgroundLocationPermission: Boolean,
    val notificationsEnabled: Boolean,
    val appVersion: String,
    val appVersionCode: Int
)

object DeviceHealthMonitor {
    fun collect(context: Context): DeviceHealthSnapshot {
        val batteryIntent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val batteryLevel = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val batteryScale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPercent = if (batteryLevel >= 0 && batteryScale > 0) {
            batteryLevel * 100 / batteryScale
        } else {
            -1
        }
        val batteryStatus = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = batteryStatus == BatteryManager.BATTERY_STATUS_CHARGING ||
            batteryStatus == BatteryManager.BATTERY_STATUS_FULL
        val rawTemperature = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        val temperature = rawTemperature.takeIf { it >= 0 }?.div(10.0)

        val memoryInfo = ActivityManager.MemoryInfo()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.getMemoryInfo(memoryInfo)

        val storage = StatFs(context.filesDir.absolutePath)
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        val networkType = when {
            capabilities == null -> "OFFLINE"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "MOBILE"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET"
            else -> "OTHER"
        }
        val networkValidated = capabilities?.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_VALIDATED
        ) == true

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val locationEnabled = runCatching {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }.getOrDefault(false)

        val foregroundLocationPermission =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
        val backgroundLocationPermission =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        return DeviceHealthSnapshot(
            collectedAt = System.currentTimeMillis(),
            batteryLevelPercent = batteryPercent,
            isCharging = isCharging,
            batteryTemperatureCelsius = temperature,
            availableStorageBytes = storage.availableBytes,
            totalStorageBytes = storage.totalBytes,
            availableMemoryBytes = memoryInfo.availMem,
            totalMemoryBytes = memoryInfo.totalMem,
            lowMemory = memoryInfo.lowMemory,
            networkType = networkType,
            networkValidated = networkValidated,
            deviceUptimeMillis = SystemClock.elapsedRealtime(),
            locationEnabled = locationEnabled,
            foregroundLocationPermission = foregroundLocationPermission,
            backgroundLocationPermission = backgroundLocationPermission,
            notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled(),
            appVersion = BuildConfig.VERSION_NAME,
            appVersionCode = BuildConfig.VERSION_CODE
        )
    }
}
