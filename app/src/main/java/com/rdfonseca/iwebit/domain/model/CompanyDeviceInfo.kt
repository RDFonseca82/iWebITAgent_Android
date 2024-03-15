package com.rdfonseca.iwebit.domain.model

import androidx.annotation.Keep

@Keep
data class CompanyDeviceInfo(
    val uniqueId: String = "",
    val idSync: String = "",
    val idCompany: String = "",
    val companyName: String = "",
    val timeSync: Int = 60,
    val timeAlive: Int = 5
)
