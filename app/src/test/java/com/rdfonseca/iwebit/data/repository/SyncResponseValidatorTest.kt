package com.rdfonseca.iwebit.data.repository

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SyncResponseValidatorTest {
    @Test
    fun acceptsEmptyAndNormalSuccessResponses() {
        assertNull(SyncResponseValidator.errorOrNull(""))
        assertNull(SyncResponseValidator.errorOrNull("OK"))
        assertNull(SyncResponseValidator.errorOrNull("{\"success\":true}"))
    }

    @Test
    fun rejectsExplicitPhpAndJsonErrors() {
        assertNotNull(SyncResponseValidator.errorOrNull("<b>Warning</b>: Undefined index"))
        assertNotNull(SyncResponseValidator.errorOrNull("SQLSTATE: connection failed"))
        assertNotNull(SyncResponseValidator.errorOrNull("{\"success\":false}"))
        assertNotNull(SyncResponseValidator.errorOrNull("{\"status\":\"error\"}"))
    }
}