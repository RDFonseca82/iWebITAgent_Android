package com.rdfonseca.iwebit.presentation.components

import android.Manifest
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.DrawerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.rdfonseca.iwebit.R
import com.rdfonseca.iwebit.common.Constants.PLAYSTORE_APP_URL
import com.rdfonseca.iwebit.common.Constants.PRIVACY_POLICY_URL
import com.rdfonseca.iwebit.common.Constants.SITE_URL
import com.rdfonseca.iwebit.presentation.Screen
import com.rdfonseca.iwebit.presentation.ui.theme.Blue100
import com.rdfonseca.iwebit.presentation.ui.theme.Blue900
import com.rdfonseca.iwebit.presentation.ui.theme.Yellow200
import com.rdfonseca.iwebit.presentation.ui.theme.Yellow600
import com.rdfonseca.iwebit.presentation.ui.theme.isDarkTheme
import com.rdfonseca.iwebit.receivers.LocationConfigChangeReceiver
import com.rdfonseca.iwebit.receivers.WifiConfigChangeReceiver
import kotlinx.coroutines.launch
import androidx.compose.material3.AlertDialog as DialogM3


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun DefaultScaffoldWithNavDrawer(
    navController: NavController,
    snackbarHostState: SnackbarHostState,
    drawerState: DrawerState,
    onLogOut: () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    var showDialog by rememberSaveable { mutableStateOf(false) }
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
    ModalNavigationDrawer(
        drawerContent = {
            ModalDrawerSheet {
                DrawerHeader()
                HorizontalDivider()
                DrawerBody(
                    onItemClick = { item ->
                        scope.launch {
                            when (item.id) {
                                Screen.DefinitionsScreen.route,
                                Screen.RecentsScreen.route,
                                Screen.SupportScreen.route,
                                Screen.WebPlatformScreen.route -> {
                                    drawerState.close()
                                    navController.navigateIfNotAlready(item.id)
                                }

                                "site" -> uriHandler.openUri(SITE_URL)
                                "rateapp" -> uriHandler.openUri(PLAYSTORE_APP_URL)
                                "logout" -> showDialog = true
                            }
                        }
                    },
                    shouldShowAlert = !(WifiConfigChangeReceiver.isWifiEnabled && LocationConfigChangeReceiver.isLocationEnabled && multiplePermissionsState.allPermissionsGranted),
                    currentScreen = navController.currentBackStackEntryAsState().value?.destination?.route
                        ?: ""
                )
            }
        },
        gesturesEnabled = drawerState.isOpen,
        drawerState = drawerState
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },

            ) {
            content(it)
        }

        if (showDialog)
            DialogM3(
                onDismissRequest = { showDialog = false },
                confirmButton = {
                    TextButton(onClick = {
                        showDialog = false
                        onLogOut()
                    }) {
                        Text(text = "Confirmar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text(text = "Cancelar")
                    }
                },
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_baseline_exit_to_app_24),
                        contentDescription = "Exit from this logged in account"
                    )
                },
                title = {
                    Text(text = "Deseja sair da conta atual?", fontSize = 18.sp)
                },
                text = {
                    Text(
                        text = "Ao confirmar, irá desassociar-se da empresa atual.",
                        textAlign = TextAlign.Center
                    )
                },
                containerColor = if (isDarkTheme()) Blue900 else Blue100
            )
    }
}


@Composable
fun DrawerHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 50.dp, horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.iwebit),
            contentDescription = "IWebIT icon",
            tint = if (isDarkTheme()) Color.White else Color.Unspecified,
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.width(20.dp))
        Text(
            text = "IWebIT",
            fontSize = 40.sp,
            color = if (isDarkTheme()) Color.White else Color.DarkGray,
            letterSpacing = 5.sp
        )
    }
}

@Composable
fun DrawerBody(
    onItemClick: (MenuItem) -> Unit,
    currentScreen: String,
    shouldShowAlert: Boolean
) {
    val uriHandler = LocalUriHandler.current

    val items = listOf(
        MenuItem(
            id = Screen.DefinitionsScreen.route,
            title = "Definições",
            contentDescription = "Definições",
            icon = painterResource(id = R.drawable.ic_outline_settings_24),
            warning = shouldShowAlert,
            isActive = currentScreen == Screen.DefinitionsScreen.route
        ),
        MenuItem(
            id = Screen.RecentsScreen.route,
            title = "Ocorrências",
            contentDescription = "Ocorrências",
            icon = painterResource(id = R.drawable.ic_baseline_assignment_24),
            isActive = currentScreen == Screen.RecentsScreen.route
        ),
        MenuItem(
            id = Screen.SupportScreen.route,
            title = "Contactar Suporte",
            contentDescription = "Contactar Suporte",
            icon = painterResource(id = R.drawable.ic_baseline_support_agent_24),
            isActive = currentScreen == Screen.SupportScreen.route
        ),
        MenuItem(
            id = Screen.WebPlatformScreen.route,
            title = "Plataforma IWebIT",
            contentDescription = "Plataforma IWebIT",
            icon = painterResource(id = R.drawable.icon_domotica),
            isActive = currentScreen == Screen.WebPlatformScreen.route
        ),
        MenuItem(
            id = "site",
            title = "Site IWebIT",
            contentDescription = "Abrir site da app",
            icon = painterResource(id = R.drawable.internet),
            isActive = currentScreen == Screen.SupportScreen.route
        ),
        MenuItem(
            id = "rateapp",
            title = "Avaliar a App",
            contentDescription = "Avaliar a App",
            icon = painterResource(id = R.drawable.ic_round_star_outline_24)
        ),
        MenuItem(
            id = "logout",
            title = "Sair",
            contentDescription = "Sair",
            icon = painterResource(id = R.drawable.ic_baseline_exit_to_app_24)
        )
    )

    LazyColumn {
        itemsIndexed(items) { index, item ->
            if (index == 0)
                Spacer(modifier = Modifier.height(8.dp))
            MenuItem(
                item = item,
                onItemClick = onItemClick
            )
        }
        item {
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { uriHandler.openUri(PRIVACY_POLICY_URL) }
                    ),
                text = buildAnnotatedString {
                    val underlineStyle = SpanStyle(textDecoration = TextDecoration.Underline)
                    append("Política de Privacidade ⋅ IWebIT")
                    addStyle(underlineStyle, start = 0, end = 23)
                },
                color = if (isDarkTheme()) Color.White else Color.Gray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun MenuItem(
    item: MenuItem,
    onItemClick: (MenuItem) -> Unit
) {
    val contentColor = if (item.warning) {
        if (isDarkTheme()) {
            Color.Yellow
        } else {
            Yellow600
        }
    } else {
        if (item.isActive) {
            MaterialTheme.colorScheme.secondary
        } else {
            if (isDarkTheme()) {
                Color.White
            } else {
                Color.Gray
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .padding(16.dp, 8.dp, 16.dp, 0.dp)
            .background(
                if (item.isActive && !item.warning)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                else if (item.isActive)
                    if (isDarkTheme())
                        Color.Yellow.copy(alpha = 0.3f)
                    else
                        Yellow200.copy(alpha = 0.3f)
                else
                    Color.Transparent,
                MaterialTheme.shapes.small
            )
            .clip(MaterialTheme.shapes.small)
            .clickable { onItemClick(item) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = if (item.warning)
                painterResource(R.drawable.ic_baseline_warning_amber_24)
            else
                item.icon,
            contentDescription = item.contentDescription,
            tint = contentColor,
            modifier = Modifier
                .size(35.dp)
                .padding(start = 10.dp)
        )

        Spacer(modifier = Modifier.width(24.dp))
        Text(
            text = item.title,
            modifier = Modifier
                .weight(1f)
                .padding(end = 10.dp),
            color = contentColor
        )
    }
}


data class MenuItem(
    val id: String,
    val title: String,
    val contentDescription: String,
    val icon: Painter,
    val warning: Boolean = false,
    var isActive: Boolean = false
)


private fun NavController.navigateIfNotAlready(id: String) {
    if (id != this.currentDestination?.route)
        this.navigate(id)
}
