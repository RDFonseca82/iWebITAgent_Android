package com.rdfonseca.iwebit.presentation.login

import androidx.compose.runtime.Immutable

@Immutable
data class LogInState(
    val error: String = "",
    val isLoading: Boolean = false
)
