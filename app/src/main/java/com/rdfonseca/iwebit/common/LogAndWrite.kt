package com.rdfonseca.iwebit.common

import android.content.Context
import android.util.Log
import com.rdfonseca.iwebit.BuildConfig
import com.rdfonseca.iwebit.common.Constants.LOG_FILE_NAME
import com.rdfonseca.iwebit.data.util.formatFromStringDateHour
import com.rdfonseca.iwebit.data.util.getDateHour
import com.rdfonseca.iwebit.presentation.TAG
import java.io.File
import java.util.*


fun logAndWrite(context: Context, message: String, file: String, method: String) {
    val logMessage = "$file:$method --> ${message.replace("\n", "")}"

    if (BuildConfig.DEBUG) {
        Log.d(TAG, logMessage)
    }

    val logFile = context.getExternalFilesDir(null)?.absolutePath?.let {
        File("$it/$LOG_FILE_NAME").also { fileObj ->
            if (!fileObj.exists()) {
                fileObj.createNewFile()
            }
        }
    }

    val isLogFileNew = (logFile?.readLines()?.size ?: 0) == 0
    Log.d(TAG, "logAndWrite: $isLogFileNew")

    if(!isLogFileNew) {
        logFile?.readLines()?.let {
            val dateParsed = formatFromStringDateHour(it[0].replace("\n", ""))
            val timeDelta = (Date().time - dateParsed.time)/1000
            val days = timeDelta/86400
            if (days > 0) {
                logFile.writeText("")
            }
        }
    }

    logFile?.appendText("${getDateHour()} | $logMessage\n")
}