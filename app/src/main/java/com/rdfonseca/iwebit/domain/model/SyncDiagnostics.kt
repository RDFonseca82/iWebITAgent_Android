package com.rdfonseca.iwebit.domain.model

data class SyncDiagnostics(
    val pendingCount: Int = 0,
    val failedCount: Int = 0,
    val lastAttemptAt: Long? = null,
    val lastSuccessAt: Long? = null,
    val lastError: String? = null
)

enum class SyncType(val key: String) {
    FULL("FULL"),
    MINIMAL("MINIMAL"),
    LOCATION("LOCATION")
}

enum class SyncFlushResult {
    SUCCESS,
    RETRY,
    FAILURE
}
