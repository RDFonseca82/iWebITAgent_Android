package com.rdfonseca.iwebit.data.identity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceIdentityTest {
    @Test
    fun permanentUniqueIdIsStableForTheSameDevice() {
        val deviceId = DeviceIdentity.sha256("android-id|signing-certificate")

        val first = DeviceIdentity.permanentUniqueId(deviceId)
        val second = DeviceIdentity.permanentUniqueId(deviceId)

        assertEquals(first, second)
        assertEquals(64, first.length)
        assertTrue(first.all { it in '0'..'9' || it in 'a'..'f' })
    }

    @Test
    fun permanentUniqueIdDoesNotDependOnNetworkData() {
        val deviceId = DeviceIdentity.sha256("android-id|signing-certificate")
        val original = DeviceIdentity.permanentUniqueId(deviceId)

        assertEquals(original, DeviceIdentity.permanentUniqueId(deviceId))
        assertNotEquals(
            original,
            DeviceIdentity.permanentUniqueId(
                DeviceIdentity.sha256("other-android-id|signing-certificate")
            )
        )
    }
}
