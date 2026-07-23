package com.rdfonseca.iwebit.presentation.update

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.rdfonseca.iwebit.presentation.ui.theme.IWebITTheme
import com.rdfonseca.iwebit.presentation.ui.theme.isDarkTheme
import com.rdfonseca.iwebit.update.AppUpdateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class UpdateInstallActivity : ComponentActivity() {
    private var status by mutableStateOf("A validar a atualização…")
    private var ready by mutableStateOf(false)
    private lateinit var apk: File
    private lateinit var expectedSha256: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        apk = File(intent.getStringExtra(EXTRA_APK_PATH).orEmpty())
        expectedSha256 = intent.getStringExtra(EXTRA_SHA256).orEmpty()
        val versionName = intent.getStringExtra(EXTRA_VERSION_NAME).orEmpty()
        val releaseNotes = intent.getStringExtra(EXTRA_RELEASE_NOTES).orEmpty()

        setContent {
            IWebITTheme(darkTheme = isDarkTheme()) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Atualização iWebIT $versionName",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        if (releaseNotes.isNotBlank()) {
                            Text(releaseNotes, modifier = Modifier.padding(top = 16.dp))
                        }
                        Text(status, modifier = Modifier.padding(vertical = 20.dp))
                        Button(enabled = ready, onClick = ::requestInstall) {
                            Text("Instalar atualização")
                        }
                    }
                }
            }
        }

        lifecycleScope.launch {
            ready = withContext(Dispatchers.IO) {
                AppUpdateManager(this@UpdateInstallActivity)
                    .validateApk(apk, expectedSha256)
            }
            status = if (ready) {
                "O APK foi validado e está pronto."
            } else {
                "A atualização é inválida ou não tem a assinatura desta aplicação."
            }
        }
    }

    private fun requestInstall() {
        if (!packageManager.canRequestPackageInstalls()) {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:$packageName")
                )
            )
            status = "Autorize a instalação por esta aplicação e volte a este ecrã."
            return
        }
        val uri = FileProvider.getUriForFile(this, "$packageName.update-files", apk)
        startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, APK_MIME_TYPE)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        )
    }

    companion object {
        const val EXTRA_APK_PATH = "update_apk_path"
        const val EXTRA_SHA256 = "update_sha256"
        const val EXTRA_VERSION_NAME = "update_version_name"
        const val EXTRA_RELEASE_NOTES = "update_release_notes"
        private const val APK_MIME_TYPE = "application/vnd.android.package-archive"
    }
}