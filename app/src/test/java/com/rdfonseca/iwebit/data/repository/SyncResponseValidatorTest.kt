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
    fun acceptsLegacyHtmlResponseContainingJavascriptErrorHandlers() {
        val response = """
            <!DOCTYPE html>
            <html>
              <head>
                <script>
                  const request = { error: function () { return false; } };
                  const initialState = { "success": false, "status": "error" };
                  window.addEventListener("error", function () {});
                </script>
              </head>
              <body>
                Welcome iWebIT Android Script
                Start Script
                UniqueID: device-id
                IdSync: company-id
              </body>
            </html>
        """.trimIndent()

        assertNull(SyncResponseValidator.errorOrNull(response))
    }

    @Test
    fun rejectsExplicitPhpAndJsonErrors() {
        assertNotNull(SyncResponseValidator.errorOrNull("<b>Warning</b>: Undefined index"))
        assertNotNull(SyncResponseValidator.errorOrNull("SQLSTATE: connection failed"))
        assertNotNull(SyncResponseValidator.errorOrNull("<html><body>Error: payload inválido</body></html>"))
        assertNotNull(SyncResponseValidator.errorOrNull("{\"success\":false}"))
        assertNotNull(SyncResponseValidator.errorOrNull("{\"status\":\"error\"}"))
    }
}
