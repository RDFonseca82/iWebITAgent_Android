package com.rdfonseca.iwebit.presentation

import android.content.Context
import android.content.IntentFilter
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.wifi.WifiManager.NETWORK_STATE_CHANGED_ACTION
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rdfonseca.iwebit.common.createNotificationChannel
import com.rdfonseca.iwebit.common.createSysTrayNotification
import com.rdfonseca.iwebit.domain.model.UserPreferences
import com.rdfonseca.iwebit.presentation.components.UserViewModel
import com.rdfonseca.iwebit.presentation.definitions.DefinitionsScreen
import com.rdfonseca.iwebit.presentation.diagnostics.DiagnosticsScreen
import com.rdfonseca.iwebit.presentation.login.LogInScreen
import com.rdfonseca.iwebit.presentation.recents.RecentsScreen
import com.rdfonseca.iwebit.presentation.support.SupportScreen
import com.rdfonseca.iwebit.presentation.ui.theme.IWebITTheme
import com.rdfonseca.iwebit.presentation.ui.theme.isDarkTheme
import com.rdfonseca.iwebit.presentation.webplataform.WebPlataform
import com.rdfonseca.iwebit.receivers.LocationConfigChangeReceiver
import com.rdfonseca.iwebit.receivers.WifiConfigChangeReceiver
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay


val LocalUserPreferences = compositionLocalOf { UserPreferences() }
const val TAG = "IWebIT»DEBUG"


@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var viewModel: UserViewModel? = null
    private var brLoc: LocationConfigChangeReceiver? = null
    private var brWifi: WifiConfigChangeReceiver? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        var showSplashScreen by mutableStateOf(true)
        installSplashScreen().apply {
            this.setKeepOnScreenCondition { showSplashScreen }
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            viewModel = hiltViewModel()

            IWebITApp(
                viewModel = viewModel!!,
                onPostSplashScreen = { showSplashScreen = false }
            )

        }
    }

    override fun onResume() {
        super.onResume()

        if (brLoc == null) {
            brLoc = LocationConfigChangeReceiver()
            val filterLocationSetting = IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
            registerReceiver(brLoc, filterLocationSetting)

            val locationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled =
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            LocationConfigChangeReceiver.isLocationEnabled = isGpsEnabled || isNetworkEnabled
        }

        if (brWifi == null) {
            brWifi = WifiConfigChangeReceiver()
            val filterWifiSetting = IntentFilter().apply {
                addAction(NETWORK_STATE_CHANGED_ACTION)
                addAction(ConnectivityManager.CONNECTIVITY_ACTION)
            }
            registerReceiver(brWifi, filterWifiSetting)
        }
    }

    override fun onPause() {
        super.onPause()

        if (brLoc != null) {
            unregisterReceiver(brLoc)
            brLoc = null
        }
        if (brWifi != null) {
            unregisterReceiver(brWifi)
            brWifi = null
        }

        if (!isChangingConfigurations) viewModel?.showToastIfChanges(this)
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IWebITApp(
    viewModel: UserViewModel,
    onPostSplashScreen: () -> Unit
) {
    val userPreferences by viewModel.userPrefsStateFlow.collectAsState(initial = UserPreferences())

    CompositionLocalProvider(
        LocalUserPreferences provides userPreferences,
        LocalOverscrollConfiguration provides null
    ) {
        IWebITTheme(darkTheme = isDarkTheme()) {
            val navController = rememberNavController()

            val context = LocalContext.current

            LaunchedEffect(true) {
                delay(100)
                onPostSplashScreen()
            }

            LaunchedEffect(userPreferences.idSync) {
                if (
                    userPreferences.idSync.isNotEmpty() &&
                    navController.currentDestination?.route == Screen.LogInScreen.route
                ) {
                    createNotificationChannel(context)
                    createSysTrayNotification(context)
                    navController.navigate(Screen.RecentsScreen.route) {
                        popUpTo(Screen.LogInScreen.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }

            Surface(color = MaterialTheme.colorScheme.background) {
                NavHost(
                    navController = navController,
                    startDestination = Screen.LogInScreen.route
                ) {
                    composable(
                        route = Screen.LogInScreen.route,
                        enterTransition = { EnterTransition.None },
                        exitTransition = {
                            fadeOut(animationSpec = tween(300))
                        }
                    ) {
                        LogInScreen(
                            navController,
                            viewModel
                        )
                    }

                    composable(
                        route = Screen.SupportScreen.route,
                        exitTransition = {
                            fadeOut(animationSpec = tween(300))
                        },
                        enterTransition = {
                            fadeIn(animationSpec = tween(300)) +
                                    slideInVertically { 300 }
                        }
                    ) {
                        SupportScreen(
                            viewModel,
                            navController
                        )
                    }

                    composable(
                        route = Screen.RecentsScreen.route,
                        exitTransition = {
                            fadeOut(animationSpec = tween(300))
                        },
                        enterTransition = {
                            fadeIn(animationSpec = tween(300))
                        }
                    ) {
                        RecentsScreen(navController, viewModel)
                    }

                    composable(
                        route = Screen.DefinitionsScreen.route,
                        exitTransition = {
                            fadeOut(animationSpec = tween(300))
                        },
                        enterTransition = {
                            fadeIn(animationSpec = tween(300))
                        }
                    ) {
                        DefinitionsScreen(
                            navController,
                            viewModel
                        )
                    }

                    composable(route = Screen.DiagnosticsScreen.route) {
                        DiagnosticsScreen(navController)
                    }

                    composable(
                        route = Screen.WebPlatformScreen.route,
                        exitTransition = {
                            fadeOut(animationSpec = tween(300)) +
                                    slideOut(
                                        animationSpec = tween(300),
                                        targetOffset = { IntOffset(0, 400) }
                                    )
                        },
                        enterTransition = {
                            fadeIn(animationSpec = tween(300)) +
                                    slideIn(
                                        animationSpec = tween(300),
                                        initialOffset = { IntOffset(0, 400) }
                                    )
                        }
                    ) {
                        WebPlataform(navController)
                    }
                }
            }
        }
    }
}

//
//if (false) Log.d(TAG, "onCreate: \n" +
//"BOARD: ${Build.BOARD} \n" +
//"BOOTLOADER: ${Build.BOOTLOADER} \n" + //
//"BRAND: ${Build.BRAND} \n" +
//"DEVICE: ${Build.DEVICE} \n" +
//"DISPLAY: ${Build.DISPLAY} \n" +
//"HARDWARE: ${Build.HARDWARE} \n" + //
//"ID: ${Build.ID} \n" +
//"MANUFACTURER: ${Build.MANUFACTURER} \n" +
//"MODEL: ${Build.MODEL} \n" +
//"ODM_SKU: ${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ) Build.ODM_SKU else ""} \n" + //
//"PRODUCT: ${Build.PRODUCT} \n" + //
//"SKU: ${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ) Build.SKU else ""} \n" + //
//"SOC_MANUFACTURER: ${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ) Build.SOC_MANUFACTURER else ""} \n" + //
//"SOC_MODEL: ${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ) Build.SOC_MODEL else ""} \n" + //
//"TIME: ${Build.TIME} \n" + //
//"TYPE: ${Build.TYPE} \n" + //
//"USER: ${Build.USER} \n" + //
//"BASE_OS: ${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ) Build.VERSION.BASE_OS else ""} \n" +
//"CODENAME: ${Build.VERSION.CODENAME} \n" +
//"INCREMENTAL: ${Build.VERSION.INCREMENTAL} \n" + //
//"MEDIA_PERFORMANCE_CLASS: ${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ) Build.VERSION.MEDIA_PERFORMANCE_CLASS else ""} \n" + //
//"PREVIEW_SDK_INT: ${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ) Build.VERSION.PREVIEW_SDK_INT else ""} \n" + //
//"RELEASE: ${Build.VERSION.RELEASE} \n" +
//"RELEASE_OR_CODENAME: ${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ) Build.VERSION.RELEASE_OR_CODENAME else ""} \n" + //
//"SDK: ${Build.VERSION.SDK} \n" + //
//"SDK_INT: ${Build.VERSION.SDK_INT} \n" + //
//"SECURITY_PATCH: ${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ) Build.VERSION.SECURITY_PATCH else ""} \n" +
//"FreeBytes: ${ssm.getFreeBytes(StorageManager.UUID_DEFAULT)} \n" + //
//"TotalBytes: ${ssm.getTotalBytes(StorageManager.UUID_DEFAULT)} \n" + //
//"AvailMem: ${meminfo.availMem} \n" + //
//"LowMemory: ${meminfo.lowMemory} \n" + //
//"Threshold: ${meminfo.threshold} \n" + //
//"TotalMem: ${meminfo.totalMem} \n" + //
//"Battery: ${level * 100 / scale} \n" +
//"MacAddress: ${getMacAdress()} \n" +
//"NetInterfaces: ${getNetInterfacesAddrs()} \n" +
//"AllApps: ${getAllApps(packageManager)} \n"
//)