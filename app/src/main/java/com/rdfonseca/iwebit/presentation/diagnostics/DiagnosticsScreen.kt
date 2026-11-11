package com.rdfonseca.iwebit.presentation.diagnostics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.rdfonseca.iwebit.common.cancelAllNotifications
import com.rdfonseca.iwebit.domain.model.DeviceHealthSnapshot
import com.rdfonseca.iwebit.presentation.Screen
import com.rdfonseca.iwebit.presentation.components.DefaultCenteredTopBar
import com.rdfonseca.iwebit.presentation.components.DefaultScaffoldWithNavDrawer
import java.text.DateFormat
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DiagnosticsScreen(
    navController: NavController,
    viewModel: DiagnosticsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        while (true) {
            viewModel.refresh()
            delay(30_000)
        }
    }

    DefaultScaffoldWithNavDrawer(
        navController = navController,
        snackbarHostState = snackbarHostState,
        drawerState = drawerState,
        onLogOut = {
            viewModel.logOut {
                cancelAllNotifications(context)
                navController.navigate(Screen.LogInScreen.route) {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            DefaultCenteredTopBar(
                onNavigationClick = { scope.launch { drawerState.open() } },
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Atualizar diagnóstico")
                    }
                }
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { HealthSummaryCard(state.health, state.sync.failedCount, state.sync.pendingCount) }
                item {
                    DiagnosticSection(
                        "Sincronização",
                        listOf(
                            "Pendentes" to state.sync.pendingCount.toString(),
                            "Falhadas" to state.sync.failedCount.toString(),
                            "Última tentativa" to formatTimestamp(state.sync.lastAttemptAt),
                            "Último sucesso" to formatTimestamp(state.sync.lastSuccessAt),
                            "Último erro" to (state.sync.lastError ?: "Sem erros")
                        )
                    )
                }
                item {
                    DiagnosticSection(
                        "Dispositivo",
                        listOf(
                            "Empresa" to state.user.companyName.ifBlank { "Não associada" },
                            "ID Sync" to state.user.idSync.ifBlank { "—" },
                            "Versão" to "${state.health.appVersion} (${state.health.appVersionCode})",
                            "Uptime" to formatDuration(state.health.deviceUptimeMillis),
                            "Bateria" to formatBattery(state.health),
                            "Temperatura" to state.health.batteryTemperatureCelsius
                                ?.let { "%.1f °C".format(it) }.orEmpty().ifBlank { "—" }
                        )
                    )
                }
                item {
                    DiagnosticSection(
                        "Recursos",
                        listOf(
                            "Armazenamento livre" to formatBytes(state.health.availableStorageBytes),
                            "Armazenamento total" to formatBytes(state.health.totalStorageBytes),
                            "Memória livre" to formatBytes(state.health.availableMemoryBytes),
                            "Memória total" to formatBytes(state.health.totalMemoryBytes),
                            "Memória baixa" to yesNo(state.health.lowMemory)
                        )
                    )
                }
                item {
                    DiagnosticSection(
                        "Conectividade e permissões",
                        listOf(
                            "Rede" to state.health.networkType,
                            "IP de saída" to (state.network?.outboundIpAddress ?: "A obter…"),
                            "Internet validada" to yesNo(state.health.networkValidated),
                            "Acesso a agent.iwebit.app" to state.network?.httpsReachable?.let(::yesNo).orEmpty().ifBlank { "A testar…" },
                            "Estado HTTP" to (state.network?.httpStatusCode?.toString() ?: "—"),
                            "Latência HTTPS" to state.network?.httpsLatencyMs?.let { "$it ms" }.orEmpty().ifBlank { "—" },
                            "Localização ativa" to yesNo(state.health.locationEnabled),
                            "Permissão de localização" to yesNo(state.health.foregroundLocationPermission),
                            "Localização em background" to yesNo(state.health.backgroundLocationPermission),
                            "Notificações" to yesNo(state.health.notificationsEnabled)
                        )
                    )
                }
                item {
                    Button(
                        onClick = {
                            viewModel.forceFullSync()
                            scope.launch { snackbarHostState.showSnackbar("Sincronização agendada") }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Sincronizar agora") }
                }
                item {
                    Text(
                        "Logs do agente (${state.logs.size} mais recentes)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                if (state.logs.isEmpty()) {
                    item { Text("Ainda não existem registos.") }
                } else {
                    items(state.logs.asReversed()) { line -> LogLine(line) }
                }
                item { Spacer(Modifier.height(12.dp)) }
            }
        }
    }
}

@Composable
private fun HealthSummaryCard(health: DeviceHealthSnapshot, failed: Int, pending: Int) {
    val warnings = buildList {
        if (!health.networkValidated) add("Sem internet validada")
        if (health.batteryLevelPercent in 0..14) add("Bateria baixa")
        if (health.lowMemory) add("Memória baixa")
        if (!health.locationEnabled || !health.foregroundLocationPermission) add("Localização indisponível")
        if (!health.notificationsEnabled) add("Notificações desativadas")
        if (failed > 0) add("$failed sincronização(ões) falhada(s)")
        if (pending > 0) add("$pending sincronização(ões) pendente(s)")
    }
    val healthy = warnings.isEmpty()
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (healthy) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                if (healthy) "Dispositivo saudável" else "Atenção necessária",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                if (healthy) "Não foram detetados problemas." else warnings.joinToString(" • "),
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

@Composable
private fun DiagnosticSection(title: String, rows: List<Pair<String, String>>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            rows.forEach { (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(label, Modifier.weight(0.46f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    SelectionContainer {
                        Text(value, Modifier.weight(0.54f), fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
private fun LogLine(line: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        SelectionContainer {
            Text(
                line,
                modifier = Modifier.padding(10.dp),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
    }
}

private fun yesNo(value: Boolean) = if (value) "Sim" else "Não"
private fun formatBattery(health: DeviceHealthSnapshot): String = when {
    health.batteryLevelPercent < 0 -> "—"
    health.isCharging -> "${health.batteryLevelPercent}% (a carregar)"
    else -> "${health.batteryLevelPercent}%"
}
private fun formatTimestamp(value: Long?): String = value?.let {
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(Date(it))
} ?: "Nunca"
private fun formatDuration(millis: Long): String {
    val days = TimeUnit.MILLISECONDS.toDays(millis)
    val hours = TimeUnit.MILLISECONDS.toHours(millis) % 24
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    return "${days}d ${hours}h ${minutes}m"
}
private fun formatBytes(bytes: Long): String {
    if (bytes < 0) return "—"
    return "%.2f GB".format(bytes / 1024.0 / 1024.0 / 1024.0)
}
