package com.rdfonseca.iwebit.domain.model

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.SystemClock
import androidx.annotation.Keep
import com.rdfonseca.iwebit.common.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetAddress
import java.net.URI
import java.util.concurrent.TimeUnit

@Keep
data class NetworkDiagnostics(
    val collectedAt: Long,
    val endpoint: String,
    val networkType: String,
    val connected: Boolean,
    val validated: Boolean,
    val metered: Boolean,
    val roaming: Boolean,
    val vpnActive: Boolean,
    val dnsServers: List<String>,
    val dnsResolved: Boolean,
    val resolvedAddresses: List<String>,
    val dnsLatencyMs: Long?,
    val httpsReachable: Boolean,
    val httpStatusCode: Int?,
    val httpsLatencyMs: Long?,
    val tlsVersion: String?,
    val error: String?
)

object NetworkDiagnosticsCollector {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .callTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun collect(context: Context): NetworkDiagnostics = withContext(Dispatchers.IO) {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        val linkProperties = connectivityManager.getLinkProperties(activeNetwork)
        val endpoint = Constants.BASE_URL
        val host = runCatching { URI(endpoint).host }.getOrNull().orEmpty()
        val errors = mutableListOf<String>()

        val dnsStartedAt = SystemClock.elapsedRealtime()
        val resolvedAddresses = if (host.isNotBlank()) {
            runCatching {
                InetAddress.getAllByName(host)
                    .mapNotNull { it.hostAddress }
                    .distinct()
            }.getOrElse {
                errors += "DNS: ${it.safeMessage()}"
                emptyList()
            }
        } else {
            errors += "DNS: endpoint inválido"
            emptyList()
        }
        val dnsLatency = if (host.isNotBlank()) {
            SystemClock.elapsedRealtime() - dnsStartedAt
        } else {
            null
        }

        var httpStatus: Int? = null
        var httpsLatency: Long? = null
        var tlsVersion: String? = null
        val httpsStartedAt = SystemClock.elapsedRealtime()
        val httpsReachable = runCatching {
            client.newCall(Request.Builder().url(endpoint).get().build()).execute().use { response ->
                httpsLatency = SystemClock.elapsedRealtime() - httpsStartedAt
                httpStatus = response.code
                tlsVersion = response.handshake?.tlsVersion?.javaName
                response.code in 200..399
            }
        }.getOrElse {
            httpsLatency = SystemClock.elapsedRealtime() - httpsStartedAt
            errors += "HTTPS: ${it.safeMessage()}"
            false
        }

        NetworkDiagnostics(
            collectedAt = System.currentTimeMillis(),
            endpoint = endpoint,
            networkType = capabilities.toNetworkType(),
            connected = capabilities != null,
            validated = capabilities?.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_VALIDATED
            ) == true,
            metered = connectivityManager.isActiveNetworkMetered,
            roaming = capabilities?.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING
            ) == false,
            vpnActive = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true,
            dnsServers = linkProperties?.dnsServers
                ?.mapNotNull { it.hostAddress }
                ?.distinct()
                .orEmpty(),
            dnsResolved = resolvedAddresses.isNotEmpty(),
            resolvedAddresses = resolvedAddresses,
            dnsLatencyMs = dnsLatency,
            httpsReachable = httpsReachable,
            httpStatusCode = httpStatus,
            httpsLatencyMs = httpsLatency,
            tlsVersion = tlsVersion,
            error = errors.takeIf { it.isNotEmpty() }?.joinToString(" | ")
        )
    }

    private fun NetworkCapabilities?.toNetworkType(): String = when {
        this == null -> "OFFLINE"
        hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
        hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
        hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "MOBILE"
        hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET"
        else -> "OTHER"
    }

    private fun Throwable.safeMessage(): String =
        localizedMessage?.take(160)?.replace(Regex("[\\r\\n]+"), " ")
            ?: javaClass.simpleName
}
