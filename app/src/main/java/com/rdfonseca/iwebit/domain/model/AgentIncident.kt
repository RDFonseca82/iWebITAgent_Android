package com.rdfonseca.iwebit.domain.model

import androidx.annotation.Keep
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Keep
data class AgentIncident(
    val code: String,
    val severity: String,
    val title: String,
    val description: String,
    val state: String = "ACTIVE",
    val source: String = "ANDROID_AGENT",
    val observedAt: Long,
    val evidence: Map<String, String> = emptyMap()
)

object IncidentDetector {
    private const val LOW_RESOURCE_PERCENT = 10.0
    private const val LOW_STORAGE_BYTES = 1_073_741_824L
    private const val HIGH_BATTERY_TEMPERATURE_CELSIUS = 45.0
    private const val SECURITY_PATCH_MAX_AGE_DAYS = 180L
    private const val STALE_SYNC_MILLIS = 6L * 60L * 60L * 1000L

    fun detect(
        health: DeviceHealthSnapshot,
        network: NetworkDiagnostics? = null,
        securityPatch: String? = null,
        syncDiagnostics: SyncDiagnostics? = null
    ): List<AgentIncident> {
        val observedAt = System.currentTimeMillis()
        val incidents = mutableListOf<AgentIncident>()
        val storagePercent = percentage(
            health.availableStorageBytes,
            health.totalStorageBytes
        )
        val memoryPercent = percentage(
            health.availableMemoryBytes,
            health.totalMemoryBytes
        )

        if (
            health.availableStorageBytes in 0 until LOW_STORAGE_BYTES ||
            storagePercent != null && storagePercent < LOW_RESOURCE_PERCENT
        ) {
            incidents += incident(
                code = "ANDROID_STORAGE_LOW",
                severity = "CRITICAL",
                title = "Armazenamento reduzido",
                description = "O dispositivo tem pouco espaço disponível.",
                observedAt = observedAt,
                evidence = mapOf(
                    "availableBytes" to health.availableStorageBytes.toString(),
                    "totalBytes" to health.totalStorageBytes.toString(),
                    "availablePercent" to storagePercent.asEvidence()
                )
            )
        }

        if (
            health.lowMemory ||
            memoryPercent != null && memoryPercent < LOW_RESOURCE_PERCENT
        ) {
            incidents += incident(
                code = "ANDROID_MEMORY_LOW",
                severity = "WARNING",
                title = "Memória reduzida",
                description = "O Android reporta pouca memória disponível.",
                observedAt = observedAt,
                evidence = mapOf(
                    "availableBytes" to health.availableMemoryBytes.toString(),
                    "totalBytes" to health.totalMemoryBytes.toString(),
                    "availablePercent" to memoryPercent.asEvidence(),
                    "systemLowMemory" to health.lowMemory.toString()
                )
            )
        }

        health.batteryTemperatureCelsius
            ?.takeIf { it >= HIGH_BATTERY_TEMPERATURE_CELSIUS }
            ?.let { temperature ->
                incidents += incident(
                    code = "ANDROID_BATTERY_TEMPERATURE_HIGH",
                    severity = "CRITICAL",
                    title = "Temperatura da bateria elevada",
                    description = "A temperatura da bateria ultrapassou o limite recomendado.",
                    observedAt = observedAt,
                    evidence = mapOf("temperatureCelsius" to temperature.toString())
                )
            }

        if (!health.networkValidated) {
            incidents += incident(
                code = "ANDROID_NETWORK_NOT_VALIDATED",
                severity = "WARNING",
                title = "Ligação à Internet não validada",
                description = "O Android não conseguiu validar o acesso à Internet.",
                observedAt = observedAt,
                evidence = mapOf("networkType" to health.networkType)
            )
        }

        if (!health.locationEnabled) {
            incidents += incident(
                code = "ANDROID_LOCATION_DISABLED",
                severity = "WARNING",
                title = "Localização desativada",
                description = "Os serviços de localização estão desativados.",
                observedAt = observedAt
            )
        }

        if (!health.foregroundLocationPermission) {
            incidents += incident(
                code = "ANDROID_LOCATION_PERMISSION_MISSING",
                severity = "WARNING",
                title = "Permissão de localização em falta",
                description = "A aplicação não tem permissão para obter a localização.",
                observedAt = observedAt
            )
        } else if (!health.backgroundLocationPermission) {
            incidents += incident(
                code = "ANDROID_BACKGROUND_LOCATION_PERMISSION_MISSING",
                severity = "WARNING",
                title = "Localização em segundo plano não permitida",
                description = "A sincronização de localização em segundo plano pode falhar.",
                observedAt = observedAt
            )
        }

        if (!health.notificationsEnabled) {
            incidents += incident(
                code = "ANDROID_NOTIFICATIONS_DISABLED",
                severity = "WARNING",
                title = "Notificações desativadas",
                description = "O agente não consegue apresentar alertas ou pedidos remotos.",
                observedAt = observedAt
            )
        }

        if (network != null && !network.dnsResolved) {
            incidents += incident(
                code = "ANDROID_AGENT_DNS_FAILURE",
                severity = "CRITICAL",
                title = "Falha de DNS para o serviço de agentes",
                description = "Não foi possível resolver o endereço do serviço iWebIT.",
                observedAt = observedAt,
                evidence = mapOf(
                    "endpoint" to network.endpoint,
                    "error" to network.error.orEmpty()
                )
            )
        } else if (network != null && !network.httpsReachable) {
            incidents += incident(
                code = "ANDROID_AGENT_HTTPS_UNREACHABLE",
                severity = "CRITICAL",
                title = "Serviço de agentes inacessível",
                description = "A ligação HTTPS ao serviço iWebIT falhou.",
                observedAt = observedAt,
                evidence = mapOf(
                    "endpoint" to network.endpoint,
                    "httpStatusCode" to network.httpStatusCode?.toString().orEmpty(),
                    "error" to network.error.orEmpty()
                )
            )
        }

        securityPatchAgeDays(securityPatch)?.takeIf {
            it > SECURITY_PATCH_MAX_AGE_DAYS
        }?.let { ageDays ->
            incidents += incident(
                code = "ANDROID_SECURITY_PATCH_OUTDATED",
                severity = "WARNING",
                title = "Patch de segurança desatualizado",
                description = "O patch de segurança Android tem mais de seis meses.",
                observedAt = observedAt,
                evidence = mapOf(
                    "securityPatch" to securityPatch.orEmpty(),
                    "ageDays" to ageDays.toString()
                )
            )
        }

        if ((syncDiagnostics?.failedCount ?: 0) > 0) {
            incidents += incident(
                code = "ANDROID_SYNC_PERMANENT_FAILURE",
                severity = "CRITICAL",
                title = "Sincronização rejeitada",
                description = "Existem dados que o servidor rejeitou permanentemente.",
                observedAt = observedAt,
                evidence = mapOf(
                    "failedCount" to syncDiagnostics?.failedCount.toString(),
                    "lastError" to syncDiagnostics?.lastError.orEmpty()
                )
            )
        }

        syncDiagnostics?.lastSuccessAt?.takeIf {
            observedAt - it > STALE_SYNC_MILLIS
        }?.let { lastSuccessAt ->
            incidents += incident(
                code = "ANDROID_SYNC_STALE",
                severity = "CRITICAL",
                title = "Dispositivo sem sincronizar",
                description = "Não existe uma sincronização concluída há mais de seis horas.",
                observedAt = observedAt,
                evidence = mapOf("lastSuccessAt" to lastSuccessAt.toString())
            )
        }

        return incidents.sortedBy { it.code }
    }

    private fun incident(
        code: String,
        severity: String,
        title: String,
        description: String,
        observedAt: Long,
        evidence: Map<String, String> = emptyMap()
    ) = AgentIncident(
        code = code,
        severity = severity,
        title = title,
        description = description,
        observedAt = observedAt,
        evidence = evidence
    )

    private fun percentage(available: Long, total: Long): Double? =
        if (available >= 0 && total > 0) available * 100.0 / total else null

    private fun Double?.asEvidence(): String =
        this?.let { "%.2f".format(java.util.Locale.US, it) }.orEmpty()

    private fun securityPatchAgeDays(securityPatch: String?): Long? =
        securityPatch
            ?.takeIf { it.isNotBlank() }
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?.let { ChronoUnit.DAYS.between(it, LocalDate.now()).coerceAtLeast(0) }
}
