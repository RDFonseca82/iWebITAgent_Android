package com.rdfonseca.iwebit.presentation.support

import androidx.compose.runtime.Immutable

@Immutable
data class UploadImageState(
    val error: String = "",
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false
)
