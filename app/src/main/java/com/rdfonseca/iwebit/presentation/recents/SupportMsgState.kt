package com.rdfonseca.iwebit.presentation.recents

import androidx.compose.runtime.Immutable
import com.rdfonseca.iwebit.domain.model.SupportMsg

@Immutable
data class SupportMsgState(
    val filteredMsgs: List<SupportMsg> = emptyList(),
    val allMsgs: List<SupportMsg> = emptyList(),
    val error: String = "",
    val isLoading: Boolean = false
)
