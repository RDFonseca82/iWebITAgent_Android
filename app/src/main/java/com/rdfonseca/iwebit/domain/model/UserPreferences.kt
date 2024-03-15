package com.rdfonseca.iwebit.domain.model

import androidx.annotation.Keep

@Keep
data class UserPreferences(
    val acceptedOrDeclinedLocPerm: Boolean = false,
    val deviceUUID: String = "",
    val lastForceFullSync: Long = 0L,
    val theme: ThemeState = ThemeState(),
    val cachedSupportNome: String = "",
    val cachedSupportSuporte: String = "",
    val uniqueId: String = "",
    val idSync: String = "",
    val idCompany: String = "",
    val companyName: String = "",
    val timeSync: Int = 0,
    val timeAlive: Int = 0
)
