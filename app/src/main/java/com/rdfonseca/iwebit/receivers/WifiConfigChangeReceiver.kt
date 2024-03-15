package com.rdfonseca.iwebit.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue


class WifiConfigChangeReceiver: BroadcastReceiver() {

    companion object {
        var isWifiEnabled by mutableStateOf(false)
            private set
    }

    override fun onReceive(context: Context, intent: Intent) {
//        Log.d(TAG, "onReceive: ${intent.action}")
        intent.action?.let { act ->
            if (act.matches("android.net.wifi.STATE_CHANGE".toRegex()) || act.matches(ConnectivityManager.CONNECTIVITY_ACTION.toRegex())) {
                val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
                var result = 0
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    cm?.run {
                        cm.getNetworkCapabilities(cm.activeNetwork)?.run {
                            if (hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                                result = 2
                            } else if (hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                                result = 1
                            } else if (hasTransport(NetworkCapabilities.TRANSPORT_VPN)){
                                result = 3
                            }
                        }
                    }
                } else {
                    cm?.run {
                        cm.activeNetworkInfo?.run {
                            when (type) {
                                ConnectivityManager.TYPE_WIFI -> {
                                    result = 2
                                }
                                ConnectivityManager.TYPE_MOBILE -> {
                                    result = 1
                                }
                                ConnectivityManager.TYPE_VPN -> {
                                    result = 3
                                }
                            }
                        }
                    }
                }

                isWifiEnabled = result != 0

//                Log.d(TAG, "onReceive: $dadosLigados $wifiLigado")

//                isWifiEnabled = dadosLigados || wifiLigado

//                val info = intent.getParcelableExtra<NetworkInfo>(WifiManager.EXTRA_NETWORK_INFO)
//                isWifiEnabled = info != null && info.isConnected
            }
        }
    }
}