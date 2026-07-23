package com.rdfonseca.iwebit.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IncidentLifecycleTest {
    private val deviceId = "ANDROID-DEVICE-001"
    private val code = "ANDROID_STORAGE_LOW"

    @Test
    fun activeIncidentKeepsStableIdAndFirstObservation() {
        val first = IncidentLifecycle.reconcile(
            deviceId = deviceId,
            previous = emptyList(),
            detected = listOf(incident(code, observedAt = 90L)),
            resolvableCodes = setOf(code),
            now = 100L
        ).single()
        val second = IncidentLifecycle.reconcile(
            deviceId = deviceId,
            previous = listOf(first),
            detected = listOf(incident(code, observedAt = 190L)),
            resolvableCodes = setOf(code),
            now = 200L
        ).single()

        assertTrue(first.id.isNotBlank())
        assertEquals(first.id, second.id)
        assertEquals(90L, second.firstObservedAt)
        assertEquals("ACTIVE", second.state)
        assertNull(second.resolvedAt)
    }

    @Test
    fun resolvedAndReopenedIncidentUsesTheSameId() {
        val active = IncidentLifecycle.reconcile(
            deviceId = deviceId,
            previous = emptyList(),
            detected = listOf(incident(code, observedAt = 90L)),
            resolvableCodes = setOf(code),
            now = 100L
        ).single()
        val resolved = IncidentLifecycle.reconcile(
            deviceId = deviceId,
            previous = listOf(active),
            detected = emptyList(),
            resolvableCodes = setOf(code),
            now = 300L
        ).single()
        val reopened = IncidentLifecycle.reconcile(
            deviceId = deviceId,
            previous = listOf(resolved),
            detected = listOf(incident(code, observedAt = 390L)),
            resolvableCodes = setOf(code),
            now = 400L
        ).single()

        assertEquals(active.id, resolved.id)
        assertEquals(active.id, reopened.id)
        assertEquals("RESOLVED", resolved.state)
        assertEquals(300L, resolved.resolvedAt)
        assertEquals("ACTIVE", reopened.state)
        assertNull(reopened.resolvedAt)
    }

    @Test
    fun minimalScopeDoesNotResolveFullOnlyIncident() {
        val fullOnlyCode = "ANDROID_AGENT_DNS_FAILURE"
        val active = IncidentLifecycle.reconcile(
            deviceId = deviceId,
            previous = emptyList(),
            detected = listOf(incident(fullOnlyCode, observedAt = 100L)),
            resolvableCodes = IncidentDetectionScope.ALL_CODES,
            now = 100L
        ).single()

        val afterMinimalSync = IncidentLifecycle.reconcile(
            deviceId = deviceId,
            previous = listOf(active),
            detected = emptyList(),
            resolvableCodes = IncidentDetectionScope.HEALTH_CODES,
            now = 200L
        ).single()

        assertEquals("ACTIVE", afterMinimalSync.state)
        assertNull(afterMinimalSync.resolvedAt)
    }

    @Test
    fun idsDifferAcrossDevicesAndIncidentCodes() {
        val first = IncidentLifecycle.stableId(deviceId, code)
        assertNotEquals(first, IncidentLifecycle.stableId("OTHER-DEVICE", code))
        assertNotEquals(first, IncidentLifecycle.stableId(deviceId, "ANDROID_MEMORY_LOW"))
    }

    private fun incident(code: String, observedAt: Long) = AgentIncident(
        code = code,
        severity = "WARNING",
        title = "Test incident",
        description = "Test incident description",
        observedAt = observedAt
    )
}