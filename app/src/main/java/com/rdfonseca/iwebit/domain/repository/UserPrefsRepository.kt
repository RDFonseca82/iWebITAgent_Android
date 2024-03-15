package com.rdfonseca.iwebit.domain.repository

import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow
import com.rdfonseca.iwebit.domain.model.DarkThemeMode
import com.rdfonseca.iwebit.domain.model.UserPreferences

interface UserPrefsRepository {

    val userPreferencesFlow: Flow<UserPreferences>

    suspend fun changeTheme(newTheme: DarkThemeMode)

    suspend fun updateCachedNome(newNome: String)
    suspend fun updateCachedSuporte(newSuporte: String)
    suspend fun updateUserInfo(
        uniqueId: String? = null,
        idSync: String? = null,
        idCompany: String? = null,
        companyName: String? = null,
        timeSync: String? = null,
        timeAlive: String? = null
    )
    suspend fun updateDeviceUUID()
    suspend fun updateLastForceFullSync()
    suspend fun updateYesOrNoLocPerm()

    fun mapUserPreferences(preferences: Preferences): UserPreferences

}