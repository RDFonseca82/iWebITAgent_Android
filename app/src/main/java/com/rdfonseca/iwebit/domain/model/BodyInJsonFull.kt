package com.rdfonseca.iwebit.domain.model

import android.app.ActivityManager
import android.app.usage.StorageStatsManager
import android.content.Context
import android.os.Build
import android.os.storage.StorageManager
import androidx.annotation.Keep
import com.rdfonseca.iwebit.BuildConfig
import com.rdfonseca.iwebit.common.getBatteryCapacity
import com.rdfonseca.iwebit.common.getIpV4
import com.rdfonseca.iwebit.common.getMacAdress
import com.rdfonseca.iwebit.data.util.getDateHour
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.UnknownHostException
import java.sql.Timestamp

@Keep
data class BodyInJsonFull(
    val UniqueId: String,
    val idsync: String,
    val TypeSync: Int,
    val version: String,
    val currentTime: String,
    val hostname: String,
    val board: String,
    val bootLoader: String,
    val brand: String,
    val device: String,
    val display: String,
    val hardware: String,
    val idAndroid: String,
    val manufacturer: String,
    val model: String,
    val odmSku: String,
    val product: String,
    val sku: String,
    val socManufacturer: String,
    val socModel: String,
    val time: String,
    val type: String,
    val user: String,
    val baseOS: String,
    val codename: String,
    val incremental: String,
    val mediaPerformanceClass: String,
    val previewSdkInt: String,
    val release: String,
    val releaseOrCodename: String,
    val sdk: String,
    val sdkInt: String,
    val securityPatch: String,
    val freeBytes: String,
    val totalBytes: String,
    val availMem: String,
    val lowMemory: String,
    val threshold: String,
    val totalMem: String,
    val batteryCapacity: String,
    val macAddress: String,
    val ipAddress: String,
    val latitude: String,
    val longitude: String,
    val AndroidProduct: MutableMap<String, Map<String, String>>,
    val health: DeviceHealthSnapshot,
    val advancedApplications: List<AdvancedApplicationInfo>,
    val applicationInventorySummary: ApplicationInventorySummary,
    val networkDiagnostics: NetworkDiagnostics,
    val incidents: List<AgentIncident>,
    val supportBundle: SupportBundle?
)

@Keep
data class BodyInJsonMin(
    val UniqueId: String,
    val idsync: String,
    val TypeSync: Int,
    val currentTime: String,
    val ipAddress: String,
    val freeBytes: String,
    val availMem: String,
    val batteryCapacity: String,
    val health: DeviceHealthSnapshot,
    val incidents: List<AgentIncident>

)

@Keep
data class BodyInJsonLocation(
    val UniqueId: String,
    val idsync: String,
    val currentTime: String,
    val latitude: String,
    val longitude: String
)


suspend fun getBodyInJsonFull(
    context: Context,
    uniqueId: String = "",
    idsync: String,
    latitude: String = "",
    longitude: String = "",
    getHostName: Boolean = false,
    syncDiagnostics: SyncDiagnostics = SyncDiagnostics(),
    includeSupportBundle: Boolean = false,
    supportBundleReason: String = "REMOTE_REQUEST"
): BodyInJsonFull {
    val ssm =
        context.getSystemService(StorageStatsManager::class.java)
    val meminfo = ActivityManager.MemoryInfo()
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    am.getMemoryInfo(meminfo)

    val ipV4 = getIpV4()

    val hostName = if (getHostName) withContext(Dispatchers.IO) {
        try {
            InetAddress.getByName(ipV4).hostName
        } catch (ex: UnknownHostException) {
            "HostNameNotFound"
        }
    } else {
        ""
    }
    val health = DeviceHealthMonitor.collect(context)
    val applicationInventory = ApplicationInventoryCollector.collect(context.packageManager)
    val networkDiagnostics = NetworkDiagnosticsCollector.collect(context)
    val detectedIncidents = IncidentDetector.detect(
        health = health,
        network = networkDiagnostics,
        securityPatch = Build.VERSION.SECURITY_PATCH,
        syncDiagnostics = syncDiagnostics
    )
    val incidents = IncidentLifecycleStore.reconcile(
        context = context,
        deviceId = uniqueId.ifBlank { idsync },
        detected = detectedIncidents,
        resolvableCodes = IncidentDetectionScope.ALL_CODES
    )
    val supportBundle = if (includeSupportBundle) {
        SupportBundleCollector.collect(
            context = context,
            reason = supportBundleReason,
            health = health,
            syncDiagnostics = syncDiagnostics,
            networkDiagnostics = networkDiagnostics,
            applicationInventorySummary = applicationInventory.summary,
            incidents = incidents.filter { it.state == "ACTIVE" }
        )
    } else {
        null
    }

    return BodyInJsonFull(
        UniqueId = uniqueId,
        idsync = idsync,
        TypeSync = 1,
        version = BuildConfig.VERSION_NAME,
        currentTime = getDateHour(),
        hostname = hostName,
        board = Build.BOARD,
        bootLoader = Build.BOOTLOADER,
        brand = Build.BRAND,
        device = Build.DEVICE,
        display = Build.DISPLAY,
        hardware = Build.HARDWARE,
        idAndroid = Build.ID,
        manufacturer = Build.MANUFACTURER,
        model = Build.MODEL,
        odmSku = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.ODM_SKU else "",
        product = Build.PRODUCT,
        sku = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.SKU else "",
        socManufacturer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.SOC_MANUFACTURER else "",
        socModel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.SOC_MODEL else "",
        time = Timestamp(Build.TIME).toString().slice(0..18),
        type = Build.TYPE,
        user = Build.USER,
        baseOS = Build.VERSION.BASE_OS,
        codename = Build.VERSION.CODENAME,
        incremental = Build.VERSION.INCREMENTAL,
        mediaPerformanceClass = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.VERSION.MEDIA_PERFORMANCE_CLASS.toString() else "0",
        previewSdkInt = Build.VERSION.PREVIEW_SDK_INT.toString(),
        release = Build.VERSION.RELEASE,
        releaseOrCodename = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Build.VERSION.RELEASE_OR_CODENAME else "",
        sdk = Build.VERSION.SDK,
        sdkInt = Build.VERSION.SDK_INT.toString(),
        securityPatch = Build.VERSION.SECURITY_PATCH,
        freeBytes = ssm?.getFreeBytes(
            StorageManager.UUID_DEFAULT
        ).toString(),
        totalBytes = ssm?.getTotalBytes(
            StorageManager.UUID_DEFAULT
        ).toString(),
        availMem = meminfo.availMem.toString(),
        lowMemory = meminfo.lowMemory.toString(),
        threshold = meminfo.threshold.toString(),
        totalMem = meminfo.totalMem.toString(),
        batteryCapacity = getBatteryCapacity(context),
        macAddress = getMacAdress(),
        ipAddress = ipV4,
        latitude = latitude,
        longitude = longitude,
        AndroidProduct = applicationInventory.toLegacyMap(),
        health = health,
        advancedApplications = applicationInventory.applications,
        applicationInventorySummary = applicationInventory.summary,
        networkDiagnostics = networkDiagnostics,
        incidents = incidents,
        supportBundle = supportBundle
    )
}


fun getBodyInJsonMin(
    context: Context,
    uniqueId: String = "",
    idsync: String,
): BodyInJsonMin {
    val ssm =
        context.getSystemService(StorageStatsManager::class.java)
    val meminfo = ActivityManager.MemoryInfo()
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    am.getMemoryInfo(meminfo)
    val health = DeviceHealthMonitor.collect(context)


    return BodyInJsonMin(
        UniqueId = uniqueId,
        idsync = idsync,
        TypeSync = 2,
        currentTime = getDateHour(),
        ipAddress = getIpV4(),
        freeBytes = ssm?.getFreeBytes(
            StorageManager.UUID_DEFAULT
        ).toString(),
        availMem = meminfo.availMem.toString(),
        batteryCapacity = getBatteryCapacity(context),
        health = health,
        incidents = IncidentLifecycleStore.reconcile(
            context = context,
            deviceId = uniqueId.ifBlank { idsync },
            detected = IncidentDetector.detect(health = health),
            resolvableCodes = IncidentDetectionScope.HEALTH_CODES
        )
    )
}


