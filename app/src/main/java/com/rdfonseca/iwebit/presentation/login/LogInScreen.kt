package com.rdfonseca.iwebit.presentation.login

import android.Manifest
import android.os.Build
import android.content.pm.PackageManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.rdfonseca.iwebit.R
import com.rdfonseca.iwebit.common.Constants
import com.rdfonseca.iwebit.common.createNotificationChannel
import com.rdfonseca.iwebit.common.createSysTrayNotification
import com.rdfonseca.iwebit.common.enqueueInitialSync
import com.rdfonseca.iwebit.common.setupFullSyncWorker
import com.rdfonseca.iwebit.common.setupMinSyncWorker
import com.rdfonseca.iwebit.domain.model.UserPreferences
import com.rdfonseca.iwebit.presentation.Screen
import com.rdfonseca.iwebit.presentation.components.UserViewModel
import com.rdfonseca.iwebit.presentation.login.components.ProminentLocationDisclosure
import com.rdfonseca.iwebit.presentation.ui.theme.isDarkTheme
import com.rdfonseca.iwebit.presentation.utils.lerp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LogInScreen(
    navController: NavController,
    userViewModel: UserViewModel
) {
    val context = LocalContext.current
    val viewModel: LogInViewModel = hiltViewModel()
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val storedUserPreferences by userViewModel.userPrefsStateFlow.collectAsState<UserPreferences, UserPreferences?>(
        initial = null
    )
    val userPreferences = storedUserPreferences ?: UserPreferences()
    var formVisible by rememberSaveable { mutableStateOf(false) }
    var showDisclosure by rememberSaveable { mutableStateOf(false) }
    var entryCompleted by rememberSaveable { mutableStateOf(false) }
    val fraction by animateFloatAsState(
        targetValue = if (formVisible) 1f else 0f,
        animationSpec = tween(400),
        label = "fractionAnimation"
    )

    fun enterApplication() {
        if (entryCompleted) return
        entryCompleted = true
        createNotificationChannel(context)
        createSysTrayNotification(context)
        setupFullSyncWorker(context)
        setupMinSyncWorker(context)
        enqueueInitialSync(context)
        navController.navigate(Screen.RecentsScreen.route) {
            popUpTo(Screen.LogInScreen.route) { inclusive = true }
            launchSingleTop = true
        }
    }

    val logInLambda = {
        scope.launch {
            viewModel.logIn {
                enterApplication()
            }
        }
        Unit
    }
    val iconOffset = lerp(0f, -200f, fraction)
    val formOffset = lerp(150f, 50f, 0.4f, 1f, fraction)
    val formAlpha = lerp(0f, 1f, 0.4f, 1f, fraction)
    val multiplePermissionsState = rememberMultiplePermissionsState(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) else listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    ) { }

    LaunchedEffect(Unit) {
        formVisible = true
        delay(400)
    }

    LaunchedEffect(storedUserPreferences) {
        val preferences = storedUserPreferences ?: return@LaunchedEffect
        val recoveredRegistration = preferences.idSync.isNotBlank()
        val notificationPermissionGranted =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
        val permissionFlowRequired =
            (!multiplePermissionsState.allPermissionsGranted || !notificationPermissionGranted) &&
                (recoveredRegistration || !preferences.acceptedOrDeclinedLocPerm)

        if (permissionFlowRequired) {
            if (!preferences.acceptedOrDeclinedLocPerm) {
                userViewModel.changeYesOrNoLocPerm()
            }
            showDisclosure = true
        } else if (recoveredRegistration) {
            enterApplication()
        }
    }

    LaunchedEffect(viewModel.state.error) {
        if (viewModel.state.error.isNotBlank()) {
            snackbarHostState.showSnackbar(viewModel.state.error)
        }
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(it),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier
                    .size(100.dp)
                    .offset(y = iconOffset.dp),
                painter = painterResource(id = R.drawable.iwebit),
                contentDescription = "Splash Icon",
                tint = if (isDarkTheme()) Color.White else Color.Unspecified
            )

            Column(
                modifier = Modifier
                    .offset(y = formOffset.dp)
                    .alpha(formAlpha),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TextField(
                    modifier = Modifier.width(320.dp),
                    value = viewModel.logInValue,
                    onValueChange = viewModel::onTextChange,
                    textStyle = LocalTextStyle.current.copy(
                        textAlign = TextAlign.Center,
                        fontSize = 18.sp
                    ),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (viewModel.logInValue.isNotBlank()) logInLambda()
                            focusManager.clearFocus()
                        }
                    ),
                    placeholder = {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = "Insira o IdSync",
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                            style = LocalTextStyle.current.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                fontSize = 18.sp,
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                )

                Button(
                    modifier = Modifier
                        .padding(top = 20.dp)
                        .height(45.dp)
                        .width(300.dp),
                    onClick = {
                        if (!viewModel.state.isLoading) {
                            logInLambda()
                        }
                    },
                    enabled = viewModel.logInValue.isNotBlank()
                ) {
                    AnimatedVisibility(visible = viewModel.state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .size(16.dp)
                                .align(Alignment.CenterVertically),
                            color = MaterialTheme.colorScheme.background,
                            strokeWidth = 2.dp
                        )
                    }
                    Text(text = "Entrar")
                }

                Text(
                    modifier = Modifier
                        .padding(top = 50.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                uriHandler.openUri(Constants.SITE_URL)
                            }
                        ),
                    text = with(AnnotatedString.Builder()) {
                        append("Não tem o seu IdSync? Clique aqui.")
                        addStyle(
                            style = SpanStyle(
                                color = MaterialTheme.colorScheme.primary
                            ),
                            start = 29,
                            end = 33
                        )
                        toAnnotatedString()
                    }
                )
            }

            AnimatedVisibility(
                visible = showDisclosure,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(200)),
            ) {
                ProminentLocationDisclosure {
                    showDisclosure = false
                    if (userPreferences.idSync.isNotBlank()) {
                        enterApplication()
                    }
                }
            }
        }
    }
}
