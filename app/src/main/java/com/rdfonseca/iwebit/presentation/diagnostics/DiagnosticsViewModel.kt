package com.rdfonseca.iwebit.presentation.diagnostics

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.BackoffPolicy
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.rdfonseca.iwebit.common.Constants
import com.rdfonseca.iwebit.common.SYNC_BACKOFF_SECONDS
import com.rdfonseca.iwebit.common.syncNetworkConstraints
import com.rdfonseca.iwebit.data.repository.OfflineSyncRepository
import com.rdfonseca.iwebit.data.repository.UserPrefsRepositoryImpl
import com.rdfonseca.iwebit.domain.model.DeviceHealthMonitor
import com.rdfonseca.iwebit.domain.model.DeviceHealthSnapshot
import com.rdfonseca.iwebit.domain.model.SyncDiagnostics
import com.rdfonseca.iwebit.domain.model.UserPreferences
import com.rdfonseca.iwebit.workers.FullSyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val offlineSyncRepository: OfflineSyncRepository,
    private val userPreferencesRepository: UserPrefsRepositoryImpl
) : ViewModel() {
    private val health = MutableStateFlow(DeviceHealthMonitor.collect(context))

    val uiState = combine(
        health,
        offlineSyncRepository.diagnostics,
        userPreferencesRepository.userPreferencesFlow
    ) { healthSnapshot, syncDiagnostics, userPreferences ->
        DiagnosticsUiState(healthSnapshot, syncDiagnostics, userPreferences)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        DiagnosticsUiState(health.value)
    )

    fun refresh() {
        health.value = DeviceHealthMonitor.collect(context)
    }

    fun forceFullSync() {
        val input = Data.Builder()
            .putBoolean(Constants.FORCE_FULL_EXTRA, true)
            .build()
        val request = OneTimeWorkRequestBuilder<FullSyncWorker>()
            .setInputData(input)
            .setConstraints(syncNetworkConstraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                SYNC_BACKOFF_SECONDS,
                TimeUnit.SECONDS
            )
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            Constants.WORKER_FORCE_SYNC_TAG,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun logOut(onComplete: () -> Unit) {
        viewModelScope.launch {
            userPreferencesRepository.updateUserInfo()
            offlineSyncRepository.clear()
            onComplete()
        }
    }
}

data class DiagnosticsUiState(
    val health: DeviceHealthSnapshot,
    val sync: SyncDiagnostics = SyncDiagnostics(),
    val user: UserPreferences = UserPreferences()
)
