package com.rdfonseca.iwebit.common

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.text.format.Formatter
import java.net.InetAddress
import java.net.NetworkInterface
import java.sql.Timestamp
import java.util.*


fun getMacAdress(): String {
    try {
        val all: List<NetworkInterface> = Collections.list(NetworkInterface.getNetworkInterfaces())
        for (nif in all) {
            if (!nif.name.equals("wlan0", ignoreCase = true)) continue
            val macBytes = nif.hardwareAddress ?: throw Exception("MacAdress empty, return default")
            val res1 = StringBuilder()
            for (b in macBytes) {
                res1.append(String.format("%02X:", b))
            }
            if (res1.isNotEmpty()) {
                res1.deleteCharAt(res1.length - 1)
            }
            return res1.toString()
        }
    } catch (ex: Exception) {
    }
    return "02:00:00:00:00:00"
}


fun getIpV4(): String {
    try {
        val interfaces: List<NetworkInterface> =
            Collections.list(NetworkInterface.getNetworkInterfaces())
        for (intf in interfaces) {
            val addrs: List<InetAddress> = Collections.list(intf.inetAddresses)
            for (addr in addrs) {
                if (!addr.isLoopbackAddress) {
                    val sAddr = addr.hostAddress ?: continue
                    val isIPv4 = sAddr.indexOf(':') < 0
                    if (isIPv4) {
                        return sAddr
                    }
                }
            }
        }
    } catch (ignored: Exception) {  }

    return ""
}


fun getAllApps(manager: PackageManager): MutableMap<String, Map<String, String>> {

    val dict = mutableMapOf<String, Map<String, String>>()

    manager.getInstalledPackages(PackageManager.GET_PERMISSIONS).forEachIndexed { index, app ->
        dict[index.toString()] = mapOf(
            "Name" to app.packageName,
            "InstallDate" to Timestamp(app.firstInstallTime).toString().slice(0..18),
            "Caption" to isSystemApp(app)
        )
    }
    return dict
}


fun getBatteryCapacity(context: Context): String {
    val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
    val batteryStatus: Intent = context.registerReceiver(null, filter)!!

    val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)

    return "${level * 100 / scale}%"
}


private fun isSystemApp(packageInfo: PackageInfo): String {
    return if (packageInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0) {
        "System"
    } else {
        "User"
    }
}





