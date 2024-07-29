package com.rdfonseca.iwebit.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue


class WifiConfigChangeReceiver : BroadcastReceiver() {

    companion object {
        var isWifiEnabled by mutableStateOf(false)
            private set
    }

    override fun onReceive(context: Context, intent: Intent) {
        intent.action?.let { act ->
            if (act.matches("android.net.wifi.STATE_CHANGE".toRegex()) || act.matches(
                    ConnectivityManager.CONNECTIVITY_ACTION.toRegex()
                )
            ) {
                val cm =
                    context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
                var result = 0
                cm?.run {
                    cm.getNetworkCapabilities(cm.activeNetwork)?.run {
                        if (hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                            result = 2
                        } else if (hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                            result = 1
                        } else if (hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                            result = 3
                        }
                    }
                }

                isWifiEnabled = result != 0
            }
        }
    }
}