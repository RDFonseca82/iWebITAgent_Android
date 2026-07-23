package com.rdfonseca.iwebit.presentation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.rdfonseca.iwebit.R
import com.rdfonseca.iwebit.common.Constants
import com.rdfonseca.iwebit.common.Constants.ACTION_UPDATE_APPS_TO_UNINSTALL
import com.rdfonseca.iwebit.common.Constants.APPLICATIONS_DATA_EXTRA
import com.rdfonseca.iwebit.common.Constants.UNINSTALL_APPS_NOTIFICATION_ID
import com.rdfonseca.iwebit.common.cancelNotification
import com.rdfonseca.iwebit.common.createUninstallAppsNotification
import com.rdfonseca.iwebit.common.getAllApps
import com.rdfonseca.iwebit.common.logAndWrite
import com.rdfonseca.iwebit.domain.model.UserPreferences
import com.rdfonseca.iwebit.presentation.components.UserViewModel
import com.rdfonseca.iwebit.presentation.ui.theme.IWebITTheme
import com.rdfonseca.iwebit.presentation.ui.theme.isDarkTheme
import com.rdfonseca.iwebit.workers.FullSyncWorker
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UninstallAppsActivity : ComponentActivity() {

    private var appsToUninstall by mutableStateOf(emptyList<String>())
    private var brUpdateAppsToUninstall = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG, "onReceive: ${intent.getStringExtra(APPLICATIONS_DATA_EXTRA)}")
            appsToUninstall =
                intent.getStringExtra(APPLICATIONS_DATA_EXTRA)?.split(",") ?: emptyList()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Log.d(TAG, "onCreate: ${intent.getStringExtra(APPLICATIONS_DATA_EXTRA)}")
        appsToUninstall = intent.getStringExtra(APPLICATIONS_DATA_EXTRA)?.split(",") ?: emptyList()
        setContentUninstallApps()

        val updateAppsToUninstallIntentFilter = IntentFilter(ACTION_UPDATE_APPS_TO_UNINSTALL)
        ContextCompat.registerReceiver(
            this,
            brUpdateAppsToUninstall,
            updateAppsToUninstallIntentFilter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(brUpdateAppsToUninstall)
    }

    @OptIn(ExperimentalFoundationApi::class)
    private fun setContentUninstallApps() {
        setContent {
            val viewModel: UserViewModel = hiltViewModel()
            val userPreferences by viewModel.userPrefsStateFlow.collectAsState(initial = UserPreferences())

            CompositionLocalProvider(
                LocalUserPreferences provides userPreferences,
                LocalOverscrollConfiguration provides null
            ) {
                IWebITTheme(isDarkTheme()) {
                    Surface(color = MaterialTheme.colorScheme.background) {
                        Column(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                text = "Desinstalar Apps",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                LazyColumn {
                                    items(appsToUninstall) { app ->
                                        val appInfo = packageManager.getApplicationInfo(app, 0)
                                        val packageLabel = appInfo.loadLabel(packageManager)
                                        val packageIcon = appInfo.loadIcon(packageManager)

                                        val painter = packageIcon?.let {
                                            rememberDrawablePainter(drawable = it)
                                        }
                                            ?: painterResource(id = R.mipmap.ic_not_found_launcher_round)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Image(
                                                modifier = Modifier.size(40.dp),
                                                painter = painter,
                                                contentDescription = "Launcher icon"
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                modifier = Modifier.weight(1f),
                                                fontSize = 20.sp,
                                                text = packageLabel.toString(),
                                                overflow = TextOverflow.Ellipsis,
                                                color = MaterialTheme.colorScheme.onBackground
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Button(onClick = { uninstallApp(app) }) {
                                                Text(text = "Remover")
                                            }
                                        }
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.onBackground.copy(
                                                alpha = 0.1f
                                            )
                                        )
                                    }
                                }
                                if (appsToUninstall.isEmpty()) {
                                    Text(
                                        modifier = Modifier.fillMaxSize(),
                                        text = "Sem aplicações a desinstalar.",
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun uninstallApp(app: String) {
        try {
            val packageUri = Uri.parse("package:$app")
            val intentDelete = Intent(Intent.ACTION_DELETE, packageUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intentDelete)
        } catch (e: Exception) {
            logAndWrite(
                this,
                "ERROR: ${e.localizedMessage}",
                "UninstallAppsActivity",
                "uninstallApp"
            )
        }
    }

    override fun onResume() {
        super.onResume()

        val currentInstalledApps = getAllApps(packageManager).map {
            it.value["Name"]
        }

        var userUninstalledApps = false

        appsToUninstall.forEach { app ->
            if (!currentInstalledApps.contains(app)) {
                userUninstalledApps = true
                appsToUninstall = appsToUninstall.filter { it != app }
            }
        }

        if (userUninstalledApps) {

            val workerData = Data.Builder().apply {
                putBoolean(Constants.FORCE_FULL_EXTRA, false)
            }

            val oneTimeWorkRequest = OneTimeWorkRequestBuilder<FullSyncWorker>()
                .setInputData(workerData.build())
                .build()

            WorkManager.getInstance(this).enqueueUniqueWork(
                Constants.WORKER_FORCE_SYNC_TAG,
                ExistingWorkPolicy.REPLACE,
                oneTimeWorkRequest
            )


        }

        if (appsToUninstall.isEmpty()) {
            cancelNotification(UNINSTALL_APPS_NOTIFICATION_ID, this)
        } else {
            createUninstallAppsNotification(this, appsToUninstall, true)
        }
    }
}