package com.rdfonseca.iwebit.common

import android.content.Context
import android.util.Log
import com.rdfonseca.iwebit.BuildConfig
import com.rdfonseca.iwebit.common.Constants.LOG_FILE_NAME
import java.io.File
import java.time.Instant

private const val LOG_TAG = "iWebITAgent"
private const val MAX_LOG_BYTES = 2L * 1024L * 1024L
private const val MAX_MESSAGE_LENGTH = 2_000
private val logLock = Any()
private val sensitiveValuePattern = Regex(
    "(?i)(authorization|password|passwd|token|secret|apikey|api_key)" +
        "(\\s*[:=]\\s*)(\\S+)"
)

fun logAndWrite(
    context: Context,
    message: String,
    file: String,
    method: String,
    level: String = "INFO"
) {
    val safeLevel = level.uppercase().take(8)
    val safeMessage = sanitizeLogValue(message)
    val source = "${file.substringAfterLast('/')}:$method"
    val line = "${Instant.now()} | $safeLevel | $source | $safeMessage"

    if (BuildConfig.DEBUG) {
        when (safeLevel) {
            "ERROR" -> Log.e(LOG_TAG, line)
            "WARN" -> Log.w(LOG_TAG, line)
            else -> Log.d(LOG_TAG, line)
        }
    }

    runCatching {
        synchronized(logLock) {
            val logFile = agentLogFile(context) ?: return@synchronized
            rotateIfNeeded(logFile)
            logFile.appendText("$line\n", Charsets.UTF_8)
        }
    }.onFailure {
        Log.e(LOG_TAG, "Não foi possível escrever no log do agente", it)
    }
}

fun readRecentAgentLogs(context: Context, maxLines: Int = 300): List<String> = runCatching {
    synchronized(logLock) {
        val logFile = agentLogFile(context)
        if (logFile == null || !logFile.isFile) {
            emptyList()
        } else {
            logFile.useLines { lines ->
                lines.toList().takeLast(maxLines.coerceIn(1, 1_000)).map(::sanitizeLogValue)
            }
        }
    }
}.getOrDefault(emptyList())

private fun agentLogFile(context: Context): File? =
    context.getExternalFilesDir(null)?.let { directory ->
        File(directory, LOG_FILE_NAME).also { file ->
            if (!file.exists()) file.createNewFile()
        }
    }

private fun rotateIfNeeded(logFile: File) {
    if (logFile.length() < MAX_LOG_BYTES) return
    val retained = logFile.useLines { lines ->
        lines.toList().takeLast(2_000).joinToString(separator = "\n", postfix = "\n")
    }
    logFile.writeText(retained, Charsets.UTF_8)
}

private fun sanitizeLogValue(value: String): String {
    val singleLine = value.replace(Regex("[\\r\\n]+"), " ").take(MAX_MESSAGE_LENGTH)
    return sensitiveValuePattern.replace(singleLine) { match ->
        "${match.groupValues[1]}${match.groupValues[2]}<redacted>"
    }
}