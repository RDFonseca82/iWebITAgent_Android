package com.rdfonseca.iwebit.data.repository

/** Detects explicit server-side failures returned with HTTP 2xx by legacy PHP endpoints. */
object SyncResponseValidator {
    private val explicitFailurePatterns = listOf(
        Regex("(?is)<b[^>]*>\\s*(?:fatal error|warning|parse error|notice)\\s*</b>"),
        Regex("(?i)\\bSQLSTATE\\b"),
        Regex("(?i)\\\"(?:success|sucesso)\\\"\\s*:\\s*false"),
        Regex("(?i)\\\"status\\\"\\s*:\\s*\\\"(?:error|failed|erro|falha)\\\"")
    )
    private val visibleFailurePattern =
        Regex("(?i)\\b(?:erro|error|failed|failure|falha)\\s*[:=]")
    private val scriptOrStylePattern =
        Regex("(?is)<(?:script|style)\\b[^>]*>.*?</(?:script|style)\\s*>")
    private val htmlTagPattern = Regex("(?s)<[^>]+>")
    private val htmlBreakPattern = Regex("(?i)<br\\s*/?>")

    fun errorOrNull(responseBody: String?): String? {
        val body = responseBody.orEmpty().trim()
        if (body.isEmpty()) return null
        // Legacy PHP endpoints may return a complete HTML page. JavaScript commonly
        // contains keys such as "error:" or "success: false", which are not failures.
        val responseWithoutScripts = body.replace(scriptOrStylePattern, " ")
        val visibleResponse = responseWithoutScripts
            .replace(htmlBreakPattern, "\n")
            .replace(htmlTagPattern, " ")
        val explicitFailure = explicitFailurePatterns.any {
            it.containsMatchIn(responseWithoutScripts)
        }
        if (!explicitFailure && !visibleFailurePattern.containsMatchIn(visibleResponse)) {
            return null
        }
        return summarize(visibleResponse)
    }

    private fun summarize(body: String): String {
        return body.replace(Regex("[\\r\\n]+"), " ").take(300)
    }
}
