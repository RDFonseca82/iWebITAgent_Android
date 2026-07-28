package com.rdfonseca.iwebit.data.identity

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import java.security.MessageDigest

object DeviceIdentity {
    private const val DEVICE_NAMESPACE = "iwebit-android-device-v2"
    private const val UNIQUE_ID_NAMESPACE = "iwebit-android-agent-v2"

    fun stableDeviceId(context: Context): String {
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ).orEmpty()
        val deviceSource = androidId.ifBlank {
            listOf(
                Build.MANUFACTURER,
                Build.MODEL,
                Build.DEVICE,
                Build.BOARD,
                Build.HARDWARE
            ).joinToString("|")
        }

        return sha256(
            "$DEVICE_NAMESPACE|$deviceSource|${signingCertificateSha256(context)}"
        )
    }

    fun permanentUniqueId(stableDeviceId: String): String =
        sha256("$UNIQUE_ID_NAMESPACE|$stableDeviceId")

    /** Preserves a valid identity already assigned to this installation. */
    fun preferredUniqueId(
        storedUniqueId: String?,
        backedUpUniqueId: String?,
        generatedUniqueId: String
    ): String = sequenceOf(storedUniqueId, backedUpUniqueId, generatedUniqueId)
        .map { it.orEmpty().trim().lowercase() }
        .firstOrNull(::isValidUniqueId)
        ?: generatedUniqueId

    internal fun isValidUniqueId(value: String): Boolean =
        value.length == 64 && value.all { it in '0'..'9' || it in 'a'..'f' }

    internal fun sha256(value: String): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    private fun signingCertificateSha256(context: Context): String {
        return runCatching {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES
                )
            }

            val certificate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo
                    ?.signingCertificateHistory
                    ?.firstOrNull()
                    ?.toByteArray()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures?.firstOrNull()?.toByteArray()
            }

            certificate?.let { bytes ->
                MessageDigest.getInstance("SHA-256")
                    .digest(bytes)
                    .joinToString("") { "%02x".format(it) }
            }.orEmpty()
        }.getOrDefault("")
    }
}