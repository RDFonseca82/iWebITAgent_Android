package com.rdfonseca.iwebit.domain.model

import android.content.Context
import com.google.gson.Gson
import java.nio.charset.StandardCharsets
import java.util.UUID

object IncidentDetectionScope {
    val HEALTH_CODES = setOf(
        "ANDROID_STORAGE_LOW",
        "ANDROID_MEMORY_LOW",
        "ANDROID_BATTERY_TEMPERATURE_HIGH",
        "ANDROID_NETWORK_NOT_VALIDATED",
        "ANDROID_LOCATION_DISABLED",
        "ANDROID_LOCATION_PERMISSION_MISSING",
        "ANDROID_BACKGROUND_LOCATION_PERMISSION_MISSING",
        "ANDROID_NOTIFICATIONS_DISABLED"
    )

    val ALL_CODES = HEALTH_CODES + setOf(
        "ANDROID_AGENT_DNS_FAILURE",
        "ANDROID_AGENT_HTTPS_UNREACHABLE",
        "ANDROID_SECURITY_PATCH_OUTDATED",
        "ANDROID_SYNC_PERMANENT_FAILURE",
        "ANDROID_SYNC_STALE"
    )
}

object IncidentLifecycle {
    private const val RESOLVED_RETENTION_MILLIS = 7L * 24L * 60L * 60L * 1000L

    fun reconcile(
        deviceId: String,
        previous: List<AgentIncident>,
        detected: List<AgentIncident>,
        resolvableCodes: Set<String>,
        now: Long = System.currentTimeMillis()
    ): List<AgentIncident> {
        require(deviceId.isNotBlank()) { "A stable device ID is required" }

        val previousByCode = previous.associateBy { it.code }
        val detectedByCode = detected.associateBy { it.code }
        val next = previous
            .filter { incident ->
                incident.state == "ACTIVE" ||
                    incident.resolvedAt?.let { now - it <= RESOLVED_RETENTION_MILLIS } == true
            }
            .associateByTo(linkedMapOf()) { it.code }

        detectedByCode.forEach { (code, incident) ->
            val old = previousByCode[code]
            next[code] = incident.copy(
                id = stableId(deviceId, code),
                state = "ACTIVE",
                observedAt = now,
                firstObservedAt = old?.firstObservedAt ?: incident.observedAt,
                resolvedAt = null
            )
        }

        resolvableCodes
            .filterNot(detectedByCode::containsKey)
            .forEach { code ->
                val old = previousByCode[code]
                if (old?.state == "ACTIVE") {
                    next[code] = old.copy(
                        id = stableId(deviceId, code),
                        state = "RESOLVED",
                        observedAt = now,
                        resolvedAt = now
                    )
                }
            }

        return next.values.sortedWith(compareBy<AgentIncident> { it.code }.thenBy { it.state })
    }

    fun stableId(deviceId: String, code: String): String =
        UUID.nameUUIDFromBytes(
            "iwebit-android-incident:$deviceId:$code"
                .toByteArray(StandardCharsets.UTF_8)
        ).toString()
}

object IncidentLifecycleStore {
    private const val PREFERENCES_NAME = "incident_lifecycle"
    private const val KEY_PREFIX = "incidents_"
    private val gson = Gson()

    @Synchronized
    fun reconcile(
        context: Context,
        deviceId: String,
        detected: List<AgentIncident>,
        resolvableCodes: Set<String>
    ): List<AgentIncident> {
        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val key = KEY_PREFIX + IncidentLifecycle.stableId(deviceId, "STATE")
        val previous = preferences.getString(key, null)
            ?.let { json ->
                runCatching {
                    gson.fromJson(json, Array<AgentIncident>::class.java).toList()
                }.getOrDefault(emptyList())
            }
            .orEmpty()
        val incidents = IncidentLifecycle.reconcile(
            deviceId = deviceId,
            previous = previous,
            detected = detected,
            resolvableCodes = resolvableCodes
        )
        preferences.edit().putString(key, gson.toJson(incidents)).commit()
        return incidents
    }
}