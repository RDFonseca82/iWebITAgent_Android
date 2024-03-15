package com.rdfonseca.iwebit.domain.model

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable

@Keep
@Immutable
data class SupportMsg(
    val header: String = "",
    val message: String = "",
    val date: String = ""
) {
    fun contains(subString: String): Boolean {
        return header.lowercase().contains(subString) || message.lowercase().contains(subString)
    }
}
