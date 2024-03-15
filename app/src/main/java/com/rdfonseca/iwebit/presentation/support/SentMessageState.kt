package com.rdfonseca.iwebit.presentation.support

import androidx.compose.runtime.Immutable

@Immutable
data class SentMessageState(
    val error: String = "",
    val isLoading: Boolean = false
)
