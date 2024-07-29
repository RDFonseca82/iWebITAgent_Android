package com.rdfonseca.iwebit.presentation.support

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.rdfonseca.iwebit.presentation.Screen
import com.rdfonseca.iwebit.presentation.components.UserViewModel
import com.rdfonseca.iwebit.presentation.support.components.SupportTextField

@Composable
fun SupportScreen(
    viewModel: UserViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val formValid = viewModel.nome.isNotBlank() && viewModel.suporte.isNotBlank()
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler {
        viewModel.showToastIfChanges(context)
        navController.popBackStack()
    }

    LaunchedEffect(key1 = viewModel.sentMessageState.error) {
        if (viewModel.sentMessageState.error.isNotBlank()) snackbarHostState.showSnackbar(
            viewModel.sentMessageState.error
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(it),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(start = 16.dp, top = 16.dp)
                ) {
                    IconButton(
                        onClick = {
                            viewModel.showToastIfChanges(context)
                            navController.popBackStack()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Get back to Recents Screen",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Text(
                        text = "Contactar Suporte",
                        fontSize = 20.sp,
                        modifier = Modifier
                            .align(CenterVertically)
                            .padding(start = 10.dp, bottom = 5.dp),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.height(45.dp))
                SupportTextField(
                    textValue = viewModel.nome,
                    onTextChange = {
                        viewModel.changeNome(it.replace("\n", ""))
                    },
                    singleLine = true,
                    placeholder = "Nome",
                    maxLength = 50,
                    imeAction = ImeAction.Next
                )
                Spacer(modifier = Modifier.height(45.dp))
                SupportTextField(
                    textValue = viewModel.suporte,
                    onTextChange = {
                        viewModel.changeSuporte(it)
                    },
                    singleLine = false,
                    placeholder = "Pedido de suporte",
                    height = 300.dp,
                    maxLength = 5000
                )
                Spacer(modifier = Modifier.height(40.dp))

                Row(modifier = Modifier.fillMaxWidth(0.85f)) {
                    Button(
                        modifier = Modifier.height(40.dp),
                        onClick = {
                            viewModel.postNewMessage { navController.navigate(Screen.RecentsScreen.route) }
                        },
                        enabled = formValid
                    ) {
                        AnimatedVisibility(visible = viewModel.sentMessageState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .padding(end = 16.dp)
                                    .size(16.dp)
                                    .align(CenterVertically),
                                color = MaterialTheme.colorScheme.background,
                                strokeWidth = 2.dp
                            )
                        }

                        Text(text = "Enviar")
                    }
                }
            }
        }
    }
}
