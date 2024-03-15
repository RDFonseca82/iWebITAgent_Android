package com.rdfonseca.iwebit.data.remote.dto

import androidx.annotation.Keep

@Keep
data class CompanyInfoDto(
    val Active: Int,
    val AgentVersion: String,
    val Company: String,
    val DeviceSupportx64: String,
    val DeviceSupportx86: String,
    val EventViewerLines: Int,
    val IDSync: String,
    val IdCompany: String,
    val LogoInactive: String,
    val LogoOffline: String,
    val LogoOnline: String,
    val TimeAlive: Int,
    val TimeSync: Int
)