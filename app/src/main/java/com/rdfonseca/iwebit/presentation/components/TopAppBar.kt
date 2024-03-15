package com.rdfonseca.iwebit.presentation.components

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.rdfonseca.iwebit.presentation.ui.theme.Yellow200
import com.rdfonseca.iwebit.presentation.ui.theme.Yellow400
import com.rdfonseca.iwebit.presentation.ui.theme.isDarkTheme
import com.rdfonseca.iwebit.receivers.LocationConfigChangeReceiver
import com.rdfonseca.iwebit.receivers.WifiConfigChangeReceiver


@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DefaultCenteredTopBar(
    modifier: Modifier = Modifier,
    onNavigationClick: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {}
) {
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

    CenterAlignedTopAppBar(
        modifier = modifier,
        title = {
            Text(
                text = "IWebIT",
                textAlign = TextAlign.Center,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        navigationIcon = {
            IconButton(
                onClick = onNavigationClick
            ) {
                BadgedBox(
                    badge = {
                        if (!(WifiConfigChangeReceiver.isWifiEnabled && LocationConfigChangeReceiver.isLocationEnabled && multiplePermissionsState.allPermissionsGranted)) {
                            Badge(
                                containerColor = if (isDarkTheme())
                                    Color.Yellow
                                else
                                    Yellow400,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ) {
                                Text("1")
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Open Menu",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}