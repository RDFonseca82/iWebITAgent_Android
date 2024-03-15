package com.rdfonseca.iwebit.data.remote.dto

import androidx.annotation.Keep

@Keep
data class UninstallAppsDtoItem(
    val IdCompany: String,
    val IdentifyingNumber: String,
    val SoftwareName: String,
    val UniqueId: String
)