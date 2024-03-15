package com.rdfonseca.iwebit.presentation.webplataform

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewState
import com.rdfonseca.iwebit.common.Constants.PLATAFORM_URL
import com.rdfonseca.iwebit.presentation.ui.theme.Gray800

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebPlataform(
    navController: NavController
) {
    val webViewState = rememberWebViewState(url = PLATAFORM_URL)

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Gray800)
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { navController.popBackStack() }
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close WebView",
                    tint = Color.White
                )
            }
        }
        WebView(
            state = webViewState,
            onCreated = { it.settings.javaScriptEnabled = true }
        )
    }


}