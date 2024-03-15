package com.rdfonseca.iwebit.presentation.definitions

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.material3.icons.Icons
import androidx.compose.material3.icons.filled.ArrowForward
import androidx.compose.material3.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.launch
import com.rdfonseca.iwebit.R
import com.rdfonseca.iwebit.common.Constants.PRIVACY_POLICY_URL
import com.rdfonseca.iwebit.common.cancelAllNotifications
import com.rdfonseca.iwebit.common.checkLocationSetting
import com.rdfonseca.iwebit.domain.model.DarkThemeMode
import com.rdfonseca.iwebit.presentation.Screen
import com.rdfonseca.iwebit.presentation.components.DefaultCenteredTopBar
import com.rdfonseca.iwebit.presentation.components.DefaultScaffoldWithNavDrawer
import com.rdfonseca.iwebit.presentation.components.UserViewModel
import com.rdfonseca.iwebit.presentation.definitions.components.CompanySettings
import com.rdfonseca.iwebit.presentation.definitions.components.Header
import com.rdfonseca.iwebit.presentation.definitions.components.UserSettings
import com.rdfonseca.iwebit.presentation.definitions.components.WarningListItem
import com.rdfonseca.iwebit.receivers.LocationConfigChangeReceiver
import com.rdfonseca.iwebit.receivers.WifiConfigChangeReceiver
import com.rdfonseca.iwebit.services.ScreenCaptureService
import com.rdfonseca.iwebit.services.SincLocationService

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun DefinitionsScreen(
    navController: NavController,
    viewModel: UserViewModel
) {
    val scaffoldState = rememberScaffoldState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val multiplePermissionsState = rememberMultiplePermissionsState(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) else listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )
    val settingResultRequest = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) {}

    DefaultScaffoldWithNavDrawer(
        navController = navController,
        scaffoldState = scaffoldState,
        onLogOut = { viewModel.logOut {
            cancelAllNotifications(context)
            context.startService(Intent(context, ScreenCaptureService::class.java))// cancel any active projections
            context.startService(Intent(context, SincLocationService::class.java))// cancel service location
            navController.popBackStack()
            navController.navigate(Screen.LogInScreen.route)
        } }
    ) { innerPaddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPaddingValues)
        ) {
            DefaultCenteredTopBar(
                onNavigationClick = {
                    scope.launch {
                        scaffoldState.drawerState.open()
                    }
                }
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp)
            ) {
                if (!(WifiConfigChangeReceiver.isWifiEnabled && LocationConfigChangeReceiver.isLocationEnabled && multiplePermissionsState.allPermissionsGranted)) {
                    item {
                        Header(title = "Avisos")
                        Divider()
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    item {
                        if (!WifiConfigChangeReceiver.isWifiEnabled) {
                            WarningListItem(
                                icon = painterResource(id = R.drawable.ic_baseline_wifi_off_24),
                                text = "Conexão de internet desligada.",
                                trailingAction = {
                                    context.startActivity(
                                        Intent(
                                            Settings.ACTION_WIFI_SETTINGS
                                        )
                                    )
                                }
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        if (!LocationConfigChangeReceiver.isLocationEnabled) {
                            WarningListItem(
                                icon = painterResource(id = R.drawable.ic_baseline_location_off_24),
                                text = "Localização desligada.",
                                trailingAction = {
                                    checkLocationSetting(
                                        context = context,
                                        onDisabled = { intentSenderRequest ->
                                            settingResultRequest.launch(intentSenderRequest)
                                        }
                                    )
                                }
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        if (!multiplePermissionsState.allPermissionsGranted) {
                            WarningListItem(
                                icon = painterResource(id = R.drawable.ic_baseline_location_off_24),
                                text = "Não permitida captura da localização.",
                                trailingAction = {
                                    context.startActivity(
                                        Intent(
                                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                            Uri.parse("package:"+context.packageName)
                                        )
                                    )
                                }
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                        Divider()
                    }
                }

                item {
                    Header(title = "Definições do Utilizador")
                    Divider()
                }

                item {
                    UserSettings(
                        onThemeChange = {
                            viewModel.changeTheme(
                                when (it) {
                                    "Claro" -> DarkThemeMode.LIGHT
                                    "Escuro" -> DarkThemeMode.DARK
                                    else -> DarkThemeMode.SYSTEM
                                }
                            )
                        }
                    )
                    Divider()
                }

                item {
                    Header(title = "Definições da Empresa")
                    Divider()
                }

                item {
                    CompanySettings()
                    Divider()
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                uriHandler.openUri(PRIVACY_POLICY_URL)
                            }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.shield),
                            contentDescription = "Privacy Policy"
                        )
                        Text(
                            modifier = Modifier.padding(start = 10.dp),
                            text = "Política de Privacidade",
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                    }
                    Divider()
                }

            }
        }
    }
}