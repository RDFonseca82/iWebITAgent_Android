package com.rdfonseca.iwebit.presentation.login.components

import android.Manifest
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.rdfonseca.iwebit.R
import com.rdfonseca.iwebit.presentation.components.LottieLoader

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ProminentLocationDisclosure(
    onDismiss: () -> Unit
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
    ) { onDismiss() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clickable(
                interactionSource = MutableInteractionSource(),
                indication = null,
                onClick = {}
            )
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Captura de Localização",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
        Text(
            text = "A aplicação IWebIT colecta a sua localização" +
                " permitindo o rastreamento do dispositivo na nossa plataforma, mesmo que a app" +
                " não esteja a ser utilzada.",
            textAlign = TextAlign.Center
        )
        LottieLoader(
            modifier = Modifier.height(270.dp),
            lottie = R.raw.gps_location_disclosure
        )
        Text(
            text = "Caso não aceite, poderá, mais tarde, alterar no painel de definições.",
            textAlign = TextAlign.Center
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = { onDismiss() }) {
                Text(text = "Não, obrigado")
            }
            Button(onClick = { multiplePermissionsState.launchMultiplePermissionRequest() }) {
                Text(text = "Aceitar")
            }
        }

    }

}
