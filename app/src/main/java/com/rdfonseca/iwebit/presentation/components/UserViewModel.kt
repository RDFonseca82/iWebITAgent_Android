package com.rdfonseca.iwebit.presentation.components

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import com.rdfonseca.iwebit.common.Resource
import com.rdfonseca.iwebit.data.repository.UserPrefsRepositoryImpl
import com.rdfonseca.iwebit.data.repository.OfflineSyncRepository
import com.rdfonseca.iwebit.domain.model.DarkThemeMode
import com.rdfonseca.iwebit.domain.use_case.PostSupportMessage
import com.rdfonseca.iwebit.domain.use_case.UploadScreenshot
import com.rdfonseca.iwebit.presentation.support.SentMessageState
import com.rdfonseca.iwebit.presentation.support.UploadImageState
import java.io.File
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(
    private val userPreferencesRepository: UserPrefsRepositoryImpl,
    private val postSupportMessage: PostSupportMessage,
    private val uploadScreenshot: UploadScreenshot,
    private val offlineSyncRepository: OfflineSyncRepository
) : ViewModel() {

    val userPrefsStateFlow = userPreferencesRepository.userPreferencesFlow

    private var messageSaverJob: Job? = null

    var sentMessageState by mutableStateOf(SentMessageState())
        private set

    var uploadImageState by mutableStateOf(UploadImageState())
        private set


    var nome by mutableStateOf("")
        private set

    private var nomeOnStart: String = ""

    var suporte by mutableStateOf("")
        private set

    private var suporteOnStart: String = ""

    init {
        viewModelScope.launch {
            userPreferencesRepository.updateDeviceUUID()
            val userPrefs = userPrefsStateFlow.first()
            nome = userPrefs.cachedSupportNome
            nomeOnStart = userPrefs.cachedSupportNome
            suporte = userPrefs.cachedSupportSuporte
            suporteOnStart = userPrefs.cachedSupportSuporte
        }
    }

    fun changeTheme(newTheme: DarkThemeMode) {
        viewModelScope.launch {
            userPreferencesRepository.changeTheme(newTheme)
        }
    }

    fun changeYesOrNoLocPerm() {
        viewModelScope.launch {
            userPreferencesRepository.updateYesOrNoLocPerm()
        }
    }

    fun changeNome(newNome: String) {
        nome = newNome
        saveMessage()
    }

    fun changeSuporte(newSuporte: String) {
        suporte = newSuporte
        saveMessage()
    }

    fun saveMessage() {
        messageSaverJob?.cancel()
        messageSaverJob = viewModelScope.launch {
            delay(400)
            userPreferencesRepository.updateCachedNome(nome.ifBlank { "" })
            userPreferencesRepository.updateCachedSuporte(suporte.ifBlank { "" })
        }
    }

    fun sendScreenShot(file: File, finishOrBackToMain: () -> Unit) {
        viewModelScope.launch {
            uploadScreenshot(file, userPrefsStateFlow.first().uniqueId).onEach { result ->
                when (result) {
                    is Resource.Success -> {
                        uploadImageState = uploadImageState.copy(
                            error = "",
                            isLoading = false,
                            isSuccess = true
                        )
                        delay(500)
                        finishOrBackToMain()
                    }
                    is Resource.Error -> {
                        uploadImageState = uploadImageState.copy(
                            error = result.message ?: "Oops, algo deu errado.",
                            isLoading = false,
                            isSuccess = false
                        )
                    }
                    is Resource.Loading -> {
                        uploadImageState = uploadImageState.copy(
                            error = "",
                            isLoading = true,
                            isSuccess = false
                        )
                    }
                }
            }.launchIn(this)
        }
    }

    fun postNewMessage(navigateBack: () -> Unit) {
        viewModelScope.launch {
            postSupportMessage(nome, suporte, userPrefsStateFlow.first().uniqueId).onEach { result ->
                when (result) {
                    is Resource.Success -> {
                        nome = ""
                        nomeOnStart = ""
                        suporte = ""
                        suporteOnStart = ""
                        userPreferencesRepository.updateCachedNome(nome)
                        userPreferencesRepository.updateCachedSuporte(suporte)
                        sentMessageState = sentMessageState.copy(
                            error = "",
                            isLoading = false
                        )
                        navigateBack()
                    }
                    is Resource.Error -> {
                        sentMessageState = sentMessageState.copy(
                            error = result.message ?: "Oops, algo deu errado.",
                            isLoading = false
                        )
                    }
                    is Resource.Loading -> {
                        sentMessageState = sentMessageState.copy(
                            error = "",
                            isLoading = true
                        )
                    }
                }
            }.launchIn(this)
        }
    }

    fun logOut(navigateToLogIn: () -> Unit) {
        viewModelScope.launch {
            userPreferencesRepository.updateUserInfo()
            offlineSyncRepository.clear()
            nome = ""
            nomeOnStart = ""
            suporte = ""
            suporteOnStart = ""
            userPreferencesRepository.updateCachedNome(nome)
            userPreferencesRepository.updateCachedSuporte(suporte)
            sentMessageState = SentMessageState()
            navigateToLogIn()
        }
    }

    fun showToastIfChanges(context: Context) {
        if (
            (nome != nomeOnStart && nome.isNotBlank()) ||
            (suporte != suporteOnStart && suporte.isNotBlank())
        ) {
            Toast.makeText(context,"A sua mensagem de suporte foi salva.", Toast.LENGTH_SHORT).show()
            nomeOnStart = nome
            suporteOnStart = suporte
        }
    }
}