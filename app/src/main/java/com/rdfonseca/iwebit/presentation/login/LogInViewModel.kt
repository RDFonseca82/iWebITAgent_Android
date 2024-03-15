package com.rdfonseca.iwebit.presentation.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import com.rdfonseca.iwebit.common.Resource
import com.rdfonseca.iwebit.data.repository.UserPrefsRepositoryImpl
import com.rdfonseca.iwebit.domain.use_case.LogIn
import javax.inject.Inject

@HiltViewModel
class LogInViewModel @Inject constructor(
    private val logInUseCase: LogIn,
    private val userPreferencesRepository: UserPrefsRepositoryImpl
) : ViewModel() {

    var state by mutableStateOf(LogInState())
        private set

    var logInValue by mutableStateOf("")
        private set

    fun onTextChange(newText: String) {
        logInValue = newText.replace("\n", "")
    }

    fun logIn(navigateTo: () -> Unit) {
        viewModelScope.launch {
            logInUseCase(
                userPreferencesRepository.userPreferencesFlow.first().deviceUUID,
                logInValue
            ).onEach { result ->
                when (result) {
                    is Resource.Success -> {
                        try {
                            result.data?.run {
                                userPreferencesRepository.updateUserInfo(
                                    uniqueId,
                                    idSync,
                                    idCompany,
                                    companyName,
                                    timeSync.toString(),
                                    timeAlive.toString()
                                )

                                navigateTo()
                            }
                            state = state.copy(
                                error = "",
                                isLoading = false
                            )
                        } catch (e: Exception) {
                            state = state.copy(
                                error = e.localizedMessage as String,
                                isLoading = false
                            )
                        }

                    }
                    is Resource.Error -> {
                        state = state.copy(
                            error = result.message?: "Oops, algo deu errado.",
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