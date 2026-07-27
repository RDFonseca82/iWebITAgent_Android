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
import com.rdfonseca.iwebit.common.logAndWrite
import com.rdfonseca.iwebit.common.readRecentAgentLogs
import com.rdfonseca.iwebit.common.syncNetworkConstraints
import com.rdfonseca.iwebit.data.repository.OfflineSyncRepository
import com.rdfonseca.iwebit.data.repository.UserPrefsRepositoryImpl
import com.rdfonseca.iwebit.domain.model.DeviceHealthMonitor
import com.rdfonseca.iwebit.domain.model.DeviceHealthSnapshot
import com.rdfonseca.iwebit.domain.model.NetworkDiagnostics
import com.rdfonseca.iwebit.domain.model.NetworkDiagnosticsCollector
import com.rdfonseca.iwebit.domain.model.SyncDiagnostics
import com.rdfonseca.iwebit.domain.model.UserPreferences
import com.rdfonseca.iwebit.workers.FullSyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val offlineSyncRepository: OfflineSyncRepository,
    private val userPreferencesRepository: UserPrefsRepositoryImpl
) : ViewModel() {
    private val health = MutableStateFlow(DeviceHealthMonitor.collect(context))
    private val network = MutableStateFlow<NetworkDiagnostics?>(null)
    private val logs = MutableStateFlow(readRecentAgentLogs(context))

    private val localDiagnostics = combine(health, network, logs) { healthValue, networkValue, logsValue ->
        LocalDiagnostics(healthValue, networkValue, logsValue)
    }

    val uiState = combine(
        localDiagnostics,
        offlineSyncRepository.diagnostics,
        userPreferencesRepository.userPreferencesFlow
    ) { local, syncDiagnostics, userPreferences ->
        DiagnosticsUiState(local.health, local.network, local.logs, syncDiagnostics, userPreferences)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        DiagnosticsUiState(health.value)
    )

    fun refresh() {
        health.value = DeviceHealthMonitor.collect(context)
        viewModelScope.launch {
            logAndWrite(context, "Diagnóstico atualizado", "DiagnosticsViewModel", "refresh")
            network.value = NetworkDiagnosticsCollector.collect(context)
            logs.value = readRecentAgentLogs(context)
        }
    }

    fun forceFullSync() {
        logAndWrite(context, "Sincronização manual solicitada", "DiagnosticsViewModel", "forceFullSync")
        val input = Data.Builder().putBoolean(Constants.FORCE_FULL_EXTRA, true).build()
        val request = OneTimeWorkRequestBuilder<FullSyncWorker>()
            .setInputData(input)
            .setConstraints(syncNetworkConstraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, SYNC_BACKOFF_SECONDS, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            Constants.WORKER_FORCE_SYNC_TAG, ExistingWorkPolicy.REPLACE, request
        )
    }

    fun logOut(onComplete: () -> Unit) {
        viewModelScope.launch {
            logAndWrite(context, "Logout autorizado; a desassociar dispositivo", "DiagnosticsViewModel", "logOut")
            userPreferencesRepository.updateUserInfo()
            offlineSyncRepository.clear()
            onComplete()
        }
    }
}

private data class LocalDiagnostics(
    val health: DeviceHealthSnapshot,
    val network: NetworkDiagnostics?,
    val logs: List<String>
)

data class DiagnosticsUiState(
    val health: DeviceHealthSnapshot,
    val network: NetworkDiagnostics? = null,
    val logs: List<String> = emptyList(),
    val sync: SyncDiagnostics = SyncDiagnostics(),
    val user: UserPreferences = UserPreferences()
)