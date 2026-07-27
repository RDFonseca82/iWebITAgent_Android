package com.rdfonseca.iwebit.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class IncidentDetectorTest {

    @Test
    fun healthyDeviceDoesNotCreateIncidents() {
        val now = System.currentTimeMillis()
        val incidents = IncidentDetector.detect(
            health = healthyHealth(),
            network = healthyNetwork(),
            securityPatch = LocalDate.now().toString(),
            syncDiagnostics = SyncDiagnostics(
                lastAttemptAt = now,
                lastSuccessAt = now
            )
        )

        assertTrue(incidents.isEmpty())
    }

    @Test
    fun unhealthyDeviceCreatesStableIncidentCodes() {
        val incidents = IncidentDetector.detect(
            health = healthyHealth().copy(
                batteryTemperatureCelsius = 48.0,
                availableStorageBytes = 256L * 1024L * 1024L,
                notificationsEnabled = false
            ),
            network = healthyNetwork().copy(
                dnsResolved = false,
                resolvedAddresses = emptyList(),
                httpsReachable = false,
                error = "DNS timeout"
            ),
            securityPatch = LocalDate.now().minusDays(365).toString(),
            syncDiagnostics = SyncDiagnostics(
                failedCount = 1,
                lastError = "HTTP 400"
            )
        )

        val codes = incidents.map { it.code }.toSet()
        assertEquals(codes.size, incidents.size)
        assertTrue("ANDROID_STORAGE_LOW" in codes)
        assertTrue("ANDROID_BATTERY_TEMPERATURE_HIGH" in codes)
        assertTrue("ANDROID_NOTIFICATIONS_DISABLED" in codes)
        assertTrue("ANDROID_AGENT_DNS_FAILURE" in codes)
        assertTrue("ANDROID_SECURITY_PATCH_OUTDATED" in codes)
        assertTrue("ANDROID_SYNC_PERMANENT_FAILURE" in codes)
    }

    private fun healthyHealth() = DeviceHealthSnapshot(
        collectedAt = System.currentTimeMillis(),
        batteryLevelPercent = 80,
        isCharging = true,
        batteryTemperatureCelsius = 30.0,
        availableStorageBytes = 64L * 1024L * 1024L * 1024L,
        totalStorageBytes = 128L * 1024L * 1024L * 1024L,
        availableMemoryBytes = 4L * 1024L * 1024L * 1024L,
        totalMemoryBytes = 8L * 1024L * 1024L * 1024L,
        lowMemory = false,
        networkType = "WIFI",
        networkValidated = true,
        deviceUptimeMillis = 1000L,
        locationEnabled = true,
        foregroundLocationPermission = true,
        backgroundLocationPermission = true,
        notificationsEnabled = true,
        appVersion = "4.3",
        appVersionCode = 403
    )

    private fun healthyNetwork() = NetworkDiagnostics(
        collectedAt = System.currentTimeMillis(),
        endpoint = "https://agent.iwebit.app/",
        networkType = "WIFI",
        connected = true,
        validated = true,
        metered = false,
        roaming = false,
        vpnActive = false,
        dnsServers = listOf("1.1.1.1"),
        dnsResolved = true,
        resolvedAddresses = listOf("203.0.113.20"),
        dnsLatencyMs = 10L,
        httpsReachable = true,
        httpStatusCode = 200,
        httpsLatencyMs = 50L,
        tlsVersion = "TLSv1.3",
        error = null
    )
}
