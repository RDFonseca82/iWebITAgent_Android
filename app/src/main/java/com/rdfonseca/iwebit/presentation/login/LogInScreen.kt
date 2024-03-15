package com.rdfonseca.iwebit.presentation.login

import android.Manifest
import android.app.Activity.RESULT_OK
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.rdfonseca.iwebit.R
import com.rdfonseca.iwebit.common.*
import com.rdfonseca.iwebit.presentation.Screen
import com.rdfonseca.iwebit.presentation.components.UserViewModel
import com.rdfonseca.iwebit.presentation.login.components.ProminentLocationDisclosure
import com.rdfonseca.iwebit.presentation.ui.theme.isDarkTheme
import com.rdfonseca.iwebit.presentation.utils.lerp

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LogInScreen(
    navController: NavController,
    userViewModel: UserViewModel
) {
    val context = LocalContext.current
    val viewModel: LogInViewModel = hiltViewModel()
    val focusManager = LocalFocusManager.current
    val scaffoldState = rememberScaffoldState()
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    var formVisible by rememberSaveable { mutableStateOf(false) }
    val fraction by animateFloatAsState(
        targetValue = if (formVisible) 1f else 0f,
        animationSpec = tween(400)
    )
    val logInLambda = {
        scope.launch {
            viewModel.logIn {
                createNotificationChannel(context)
                createSysTrayNotification(context)
                setupFullSyncWorker(context)
                setupMinSyncWorker(context)
                navController.popBackStack()
                navController.navigate(Screen.RecentsScreen.route)
            }
        }
        Unit
    }
    val iconOffset = lerp(0f, -200f, fraction)
    val formOffset = lerp(150f, 50f, 0.4f, 1f, fraction)
    val formAlpha = lerp(0f, 1f, 0.4f, 1f, fraction)
    var showDisclosure by rememberSaveable { mutableStateOf(false) }
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

    LaunchedEffect(key1 = true) {
        formVisible = true
        delay(400)
        if (!userViewModel.userPrefsStateFlow.first().acceptedOrDeclinedLocPerm && !multiplePermissionsState.allPermissionsGranted) {
            userViewModel.changeYesOrNoLocPerm()
            showDisclosure = true
        }
    }

    LaunchedEffect(key1 = viewModel.state.error) {
        if (viewModel.state.error.isNotBlank()) scaffoldState.snackbarHostState.showSnackbar(viewModel.state.error)
    }

    Scaffold(
        scaffoldState = scaffoldState
    ){
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
                    colors = TextFieldDefaults.textFieldColors(
                        backgroundColor = Color.Transparent
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
                            interactionSource = MutableInteractionSource(),
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
                }
            }
        }
    }
}