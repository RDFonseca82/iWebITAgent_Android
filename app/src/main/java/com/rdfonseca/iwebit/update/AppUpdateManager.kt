package com.rdfonseca.iwebit.update

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.google.gson.Gson
import com.rdfonseca.iwebit.BuildConfig
import com.rdfonseca.iwebit.domain.model.AppUpdate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class AppUpdateManager(
    private val context: Context,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(2, TimeUnit.MINUTES)
        .followRedirects(true)
        .build()
) {
    suspend fun findUpdate(): AppUpdate? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(BuildConfig.UPDATE_MANIFEST_URL)
            .header("Accept", "application/json")
            .header("User-Agent", "iWebIT-Agent/${BuildConfig.VERSION_NAME}")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Manifesto de atualização: HTTP ${response.code}")
            val update = Gson().fromJson(response.body?.charStream(), AppUpdate::class.java)
                ?: error("Manifesto de atualização vazio")
            require(update.schemaVersion == 1) { "Versão de manifesto não suportada" }
            require(update.versionCode > 0 && update.versionName.isNotBlank()) {
                "Versão de atualização inválida"
            }
            require(update.apkUrl.startsWith(ALLOWED_APK_URL_PREFIX)) {
                "Origem do APK não autorizada"
            }
            require(SHA256_REGEX.matches(update.sha256)) { "SHA-256 inválido" }
            update.takeIf { it.versionCode > BuildConfig.VERSION_CODE }
        }
    }

    suspend fun downloadAndValidate(update: AppUpdate): File = withContext(Dispatchers.IO) {
        val updateDirectory = File(context.filesDir, UPDATE_DIRECTORY).apply { mkdirs() }
        val destination = File(updateDirectory, apkFileName(update.versionCode))
        updateDirectory.listFiles()?.forEach { file ->
            if (file != destination) file.delete()
        }
        if (destination.isFile && validateApk(destination, update.sha256)) {
            return@withContext destination
        }

        val partial = File(updateDirectory, "${destination.name}.part")
        partial.delete()
        val request = Request.Builder()
            .url(update.apkUrl)
            .header("User-Agent", "iWebIT-Agent/${BuildConfig.VERSION_NAME}")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("Download do APK: HTTP ${response.code}")
                val body = response.body ?: error("APK vazio")
                if (body.contentLength() > MAX_APK_BYTES) error("APK excede o tamanho permitido")
                FileOutputStream(partial).use { output ->
                    body.byteStream().use { input -> input.copyTo(output) }
                }
            }
            if (partial.length() <= 0 || partial.length() > MAX_APK_BYTES) {
                error("Tamanho do APK inválido")
            }
            if (update.fileSize > 0 && partial.length() != update.fileSize) {
                error("Tamanho do APK não corresponde ao manifesto")
            }
            if (!validateApk(partial, update.sha256)) {
                error("O APK não passou a validação de integridade ou assinatura")
            }
            check(partial.renameTo(destination)) { "Não foi possível guardar o APK" }
            destination
        } catch (error: Exception) {
            partial.delete()
            throw error
        }
    }

    fun validateApk(apk: File, expectedSha256: String): Boolean {
        if (!apk.isFile || sha256(apk) != expectedSha256.lowercase()) return false
        val archive = getPackageInfo(apk.absolutePath) ?: return false
        if (archive.packageName != context.packageName) return false
        val installed = getPackageInfo(context.packageName, installed = true) ?: return false
        return signingCertificates(archive).isNotEmpty() &&
            signingCertificates(archive) == signingCertificates(installed)
    }

    private fun getPackageInfo(value: String, installed: Boolean = false): PackageInfo? {
        val packageManager = context.packageManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val flags = PackageManager.PackageInfoFlags.of(
                PackageManager.GET_SIGNING_CERTIFICATES.toLong()
            )
            if (installed) packageManager.getPackageInfo(value, flags)
            else packageManager.getPackageArchiveInfo(value, flags)
        } else {
            @Suppress("DEPRECATION")
            if (installed) packageManager.getPackageInfo(value, PackageManager.GET_SIGNING_CERTIFICATES)
            else packageManager.getPackageArchiveInfo(value, PackageManager.GET_SIGNING_CERTIFICATES)
        }
    }

    private fun signingCertificates(info: PackageInfo): Set<String> {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = info.signingInfo ?: return emptySet()
            if (signingInfo.hasMultipleSigners()) signingInfo.apkContentsSigners
            else signingInfo.signingCertificateHistory
        } else {
            @Suppress("DEPRECATION")
            info.signatures
        }
        return signatures.orEmpty().map { signature ->
            MessageDigest.getInstance("SHA-256")
                .digest(signature.toByteArray())
                .joinToString("") { "%02x".format(it) }
        }.toSet()
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val UPDATE_DIRECTORY = "updates"
        private const val MAX_APK_BYTES = 200L * 1024L * 1024L
        private const val ALLOWED_APK_URL_PREFIX =
            "https://github.com/RDFonseca82/iWebITAgent_Android/releases/download/"
        private val SHA256_REGEX = Regex("^[a-fA-F0-9]{64}$")

        fun apkFileName(versionCode: Long) = "iwebit-agent-$versionCode.apk"
    }
}