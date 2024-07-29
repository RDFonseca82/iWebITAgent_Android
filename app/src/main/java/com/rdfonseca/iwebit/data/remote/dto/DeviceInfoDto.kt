package com.rdfonseca.iwebit.data.remote.dto

import androidx.annotation.Keep

@Keep
data class DeviceInfoDto(
    val Active: Int,
    val Message: Any,
    val MessageTxt: String?,
    val AndroidPrintScreen: Int,
    val DeviceLocation: Int,
    val FullSync: Int,
    val IDSync: String,
    val IdCompany: String,
    val IdDevice: String,
    val IdDeviceType: Int,
    val LastConnectDate: String,
    val MSSQLServer: Any,
    val OperatingSystem_Reboot: Int,
    val OperatingSystem_ShutDown: Int,
    val TimeAlive: Int,
    val TimeSync: Int,
    val UniqueID: String
)