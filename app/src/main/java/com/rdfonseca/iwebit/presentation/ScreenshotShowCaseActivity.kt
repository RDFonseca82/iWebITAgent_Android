package com.rdfonseca.iwebit.presentation

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import com.rdfonseca.iwebit.presentation.components.UserViewModel
import java.io.File

@AndroidEntryPoint
class ScreenshotShowCaseActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentCapturedScreen()
    }

    private fun setContentCapturedScreen() {
        setContent {
            val viewModel: UserViewModel = hiltViewModel()
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                val externalFilesDir = getExternalFilesDir(null)
                if (externalFilesDir != null) {
                    val path = externalFilesDir.absolutePath + "/screenshots/" + "ScreenShot.png"
                    val file = File(path)
                    if (!file.exists()) {
                        finish()
                    }
                    val bmp = BitmapFactory.decodeFile(path)

                    Image(
                        modifier = Modifier
                            .fillMaxSize()
                            .align(Alignment.Center),
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "ScreenShot Image"
                    )
                    IconButton(
                        modifier = Modifier
                            .padding(10.dp)
                            .align(Alignment.TopStart)
                            .background(Color(0x44444444), CircleShape),
                        onClick = {
                            if (file.exists()) file.delete()
                            finishAndRemoveTask()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "reject screenshot",
                            tint = Color.White
                        )
                    }
                    IconButton(
                        modifier = Modifier
                            .padding(20.dp)
                            .align(Alignment.TopEnd)
                            .background(Color(0x44444444), CircleShape),
                        onClick = {
                            viewModel.sendScreenShot(file) { finishAndRemoveTask() }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "accept screenshot",
                            tint = Color.White
                        )
                    }
                }
                if (viewModel.uploadImageState.isLoading) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .height(70.dp)
                            .background(Color(0xEE444444), RoundedCornerShape(10.dp))
                            .padding(15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "A enviar imagem...",
                            color = Color.White
                        )
                    }
                }
                if (viewModel.uploadImageState.isSuccess) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .height(70.dp)
                            .background(Color(0xEE444444), RoundedCornerShape(10.dp))
                            .padding(15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Screenshot sent with success",
                            tint = Color.Green
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Imagem enviada com sucesso.",
                            color = Color.White
                        )
                    }
                }
            }
        }
    }

}