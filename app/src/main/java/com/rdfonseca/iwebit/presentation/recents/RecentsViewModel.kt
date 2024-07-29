package com.rdfonseca.iwebit.presentation.recents

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rdfonseca.iwebit.common.Resource
import com.rdfonseca.iwebit.data.repository.UserPrefsRepositoryImpl
import com.rdfonseca.iwebit.domain.use_case.GetSupportMessages
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecentsViewModel @Inject constructor(
    userPreferencesRepository: UserPrefsRepositoryImpl,
    private val getSupportMessages: GetSupportMessages
) : ViewModel() {

    val userPrefsStateFlow = userPreferencesRepository.userPreferencesFlow
    private var searchJob: Job? = null
    private var getMessagesJob: Job? = null

    var state by mutableStateOf(SupportMsgState())
        private set

    var searchText by mutableStateOf("")
        private set

    init {
        refreshSupportedMessages()
    }

    fun onSearch(newText: String) {
        searchJob?.cancel()

        if (newText.isNotBlank()) searchJob = viewModelScope.launch {
            delay(200)
            state = state.copy(
                filteredMsgs = state.allMsgs.filter { it.contains(newText.lowercase()) },
                isLoading = false
            )
        } else {
            state = state.copy(
                filteredMsgs = emptyList(),
                isLoading = false
            )
        }
        searchText = newText
    }

    fun refreshSupportedMessages() {
        getMessagesJob?.cancel()
        getMessagesJob = viewModelScope.launch {
            getSupportMessages(userPrefsStateFlow.first().uniqueId).onEach { result ->
                when (result) {
                    is Resource.Success -> {
                        state = state.copy(
                            error = "",
                            allMsgs = result.data ?: emptyList(),
                            isLoading = false
                        )
                    }

                    is Resource.Error -> {
                        state = state.copy(
                            error = result.message ?: "Oops, algo deu errado.",
                            isLoading = false
                        )
                    }

                    is Resource.Loading -> {
                        state = state.copy(
                            error = "",
                            isLoading = true
                        )
                    }
                }
            }.launchIn(this)
        }
    }
}