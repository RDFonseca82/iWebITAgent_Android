package com.rdfonseca.iwebit.presentation.login.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import com.rdfonseca.iwebit.R
import com.rdfonseca.iwebit.presentation.components.LottieLoader

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ProminentLocationDisclosure(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val foregroundPermissions = listOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    val foregroundAlreadyGranted = foregroundPermissions.any { permission ->
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
    val backgroundAlreadyGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    var showBackgroundInstructions by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                foregroundAlreadyGranted &&
                !backgroundAlreadyGranted
        )
    }

    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        onDismiss()
    }
    val backgroundPermission = rememberPermissionState(
        Manifest.permission.ACCESS_BACKGROUND_LOCATION
    ) {
        onDismiss()
    }
    val foregroundPermission = rememberMultiplePermissionsState(
        foregroundPermissions
    ) { results ->
        val granted = results.values.any { it }
        when {
            !granted -> onDismiss()
            Build.VERSION.SDK_INT == Build.VERSION_CODES.Q -> {
                backgroundPermission.launchPermissionRequest()
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                showBackgroundInstructions = true
            }
            else -> onDismiss()
        }
    }

    fun requestLocationPermission() {
        when {
            backgroundAlreadyGranted -> onDismiss()
            showBackgroundInstructions -> {
                settingsLauncher.launch(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                )
            }
            foregroundAlreadyGranted && Build.VERSION.SDK_INT == Build.VERSION_CODES.Q -> {
                backgroundPermission.launchPermissionRequest()
            }
            foregroundAlreadyGranted -> onDismiss()
            else -> foregroundPermission.launchMultiplePermissionRequest()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Localização do dispositivo",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
        Text(
            text = if (showBackgroundInstructions) {
                "Para concluir, abra as definições da aplicação, selecione Localização e escolha " +
                    "Permitir sempre. Esta opção é necessária para localizar o dispositivo quando " +
                    "a aplicação está fechada ou não está a ser utilizada."
            } else {
                "O iWebIT Agent recolhe dados de localização para permitir que a sua organização " +
                    "localize e proteja este dispositivo na plataforma iWebIT, mesmo quando a " +
                    "aplicação está fechada ou não está a ser utilizada. Os dados são transmitidos " +
                    "de forma segura para a plataforma iWebIT e não são usados para publicidade."
            },
            textAlign = TextAlign.Center
        )
        LottieLoader(
            modifier = Modifier.height(270.dp),
            lottie = R.raw.gps_location_disclosure
        )
        Text(
            text = "A permissão é opcional e pode ser alterada mais tarde nas definições do Android.",
            textAlign = TextAlign.Center
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onDismiss) {
                Text(text = "Agora não")
            }
            Button(onClick = ::requestLocationPermission) {
                Text(text = if (showBackgroundInstructions) "Abrir definições" else "Continuar")
            }
        }
    }
}