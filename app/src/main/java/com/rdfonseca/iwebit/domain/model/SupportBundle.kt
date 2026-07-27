package com.rdfonseca.iwebit.domain.model

import android.content.Context
import android.os.Build
import androidx.annotation.Keep
import com.rdfonseca.iwebit.BuildConfig
import com.rdfonseca.iwebit.common.readRecentAgentLogs

@Keep
data class SupportBundle(
    val generatedAt: Long,
    val reason: String,
    val appVersion: String,
    val appVersionCode: Int,
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val androidSdk: Int,
    val securityPatch: String,
    val health: DeviceHealthSnapshot,
    val syncDiagnostics: SyncDiagnostics,
    val networkDiagnostics: NetworkDiagnostics,
    val applicationInventorySummary: ApplicationInventorySummary,
    val activeIncidents: List<AgentIncident>,
    val recentLogs: List<String>
)

object SupportBundleCollector {
    private const val MAX_LOG_LINES = 100
    private const val MAX_LOG_LINE_LENGTH = 500
    private val secretPattern = Regex(
        pattern = "(?i)(authorization|password|passwd|token|secret|apikey|api_key)" +
            "(\\s*[:=]\\s*)(\\S+)"
    )

    fun collect(
        context: Context,
        reason: String,
        health: DeviceHealthSnapshot,
        syncDiagnostics: SyncDiagnostics,
        networkDiagnostics: NetworkDiagnostics,
        applicationInventorySummary: ApplicationInventorySummary,
        incidents: List<AgentIncident>
    ): SupportBundle = SupportBundle(
        generatedAt = System.currentTimeMillis(),
        reason = reason,
        appVersion = BuildConfig.VERSION_NAME,
        appVersionCode = BuildConfig.VERSION_CODE,
        manufacturer = Build.MANUFACTURER,
        model = Build.MODEL,
        androidVersion = Build.VERSION.RELEASE,
        androidSdk = Build.VERSION.SDK_INT,
        securityPatch = Build.VERSION.SECURITY_PATCH,
        health = health,
        syncDiagnostics = syncDiagnostics,
        networkDiagnostics = networkDiagnostics,
        applicationInventorySummary = applicationInventorySummary,
        activeIncidents = incidents,
        recentLogs = readRecentLogs(context)
    )

    private fun readRecentLogs(context: Context): List<String> {
        return readRecentAgentLogs(context, MAX_LOG_LINES).map { line ->
            secretPattern.replace(line.take(MAX_LOG_LINE_LENGTH)) { match ->
                "${match.groupValues[1]}${match.groupValues[2]}<redacted>"
            }
        }
    }
}
