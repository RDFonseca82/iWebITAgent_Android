package com.rdfonseca.iwebit.data.repository

/** Detects explicit server-side failures returned with HTTP 2xx by legacy PHP endpoints. */
object SyncResponseValidator {
    private val explicitFailurePatterns = listOf(
        Regex("(?i)<(?:b>)?(?:fatal error|warning|parse error|notice)"),
        Regex("(?i)\\bSQLSTATE\\b"),
        Regex("(?i)\\b(?:erro|error|failed|failure|falha)\\s*[:=]"),
        Regex("(?i)\\\"(?:success|sucesso)\\\"\\s*:\\s*false"),
        Regex("(?i)\\\"status\\\"\\s*:\\s*\\\"(?:error|failed|erro|falha)\\\"")
    )

    fun errorOrNull(responseBody: String?): String? {
        val body = responseBody.orEmpty().trim()
        if (body.isEmpty()) return null
        if (explicitFailurePatterns.none { it.containsMatchIn(body) }) return null
        return body.replace(Regex("[\\r\\n]+"), " ").take(300)
    }
}