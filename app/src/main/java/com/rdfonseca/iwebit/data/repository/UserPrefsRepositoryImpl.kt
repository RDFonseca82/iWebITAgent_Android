package com.rdfonseca.iwebit.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import com.rdfonseca.iwebit.common.logAndWrite
import com.rdfonseca.iwebit.domain.model.DarkThemeMode
import com.rdfonseca.iwebit.domain.model.ThemeState
import com.rdfonseca.iwebit.domain.model.UserPreferences
import com.rdfonseca.iwebit.domain.repository.UserPrefsRepository
import java.io.IOException
import java.util.*
import javax.inject.Inject

class UserPrefsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val context: Context
) : UserPrefsRepository {

    private object PreferencesKeys {
        val YES_OR_NO_LOC_PERM = booleanPreferencesKey("yes_or_no_loc_perm")
        val DEVICE_UUID = stringPreferencesKey("device_uuid")
        val LAST_FORCE_FULL_SYNC = longPreferencesKey("last_force_full_sync")
        val DARK_THEME = stringPreferencesKey("dark_theme")
        val SUPPORT_NOME = stringPreferencesKey("support_nome")
        val SUPPORT_SUPORTE = stringPreferencesKey("support_suporte")
        val UNIQUE_ID = stringPreferencesKey("unique_id")
        val ID_SYNC = stringPreferencesKey("id_sync")
        val ID_COMPANY = stringPreferencesKey("id_company")
        val COMPANY_NAME = stringPreferencesKey("company_name")
        val TIME_SYNC = stringPreferencesKey("time_sync")
        val TIME_ALIVE = stringPreferencesKey("time_alive")
    }

    override val userPreferencesFlow: Flow<UserPreferences> = dataStore.data
    .catch { exception ->
        if (exception is IOException) {
            logAndWrite(context, "ERROR: $exception", "UserPrefsRepositoryImpl.kt", "val_userPreferencesFlow")
            emit(emptyPreferences())
        } else {
            throw exception
        }
    }.map { preferences ->
        mapUserPreferences(preferences)
    }

    override suspend fun changeTheme(newTheme: DarkThemeMode) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DARK_THEME] = newTheme.name
        }
    }

    override suspend fun updateCachedNome(newNome: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SUPPORT_NOME] = newNome
        }
    }

    override suspend fun updateCachedSuporte(newSuporte: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SUPPORT_SUPORTE] = newSuporte
        }
    }

    override suspend fun updateUserInfo(
        uniqueId: String?,
        idSync: String?,
        idCompany: String?,
        companyName: String?,
        timeSync: String?,
        timeAlive: String?
    ) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.UNIQUE_ID] = uniqueId ?: ""
            preferences[PreferencesKeys.ID_SYNC] = idSync ?: ""
            preferences[PreferencesKeys.ID_COMPANY] = idCompany ?: ""
            preferences[PreferencesKeys.COMPANY_NAME] = companyName ?: ""
            preferences[PreferencesKeys.TIME_SYNC] = timeSync ?: "0"
            preferences[PreferencesKeys.TIME_ALIVE] = timeAlive ?: "0"
        }
    }

    override suspend fun updateDeviceUUID() {
        dataStore.edit { preferences ->
            if (preferences[PreferencesKeys.DEVICE_UUID] == null)
                preferences[PreferencesKeys.DEVICE_UUID] = UUID.randomUUID().toString()
        }
    }

    override suspend fun updateLastForceFullSync() {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_FORCE_FULL_SYNC] = Date().time
        }
    }

    override suspend fun updateYesOrNoLocPerm() {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.YES_OR_NO_LOC_PERM] = true
        }
    }

    override fun mapUserPreferences(preferences: Preferences): UserPreferences {
        val acceptedOrDeclinedLocPerm = preferences[PreferencesKeys.YES_OR_NO_LOC_PERM] ?: false
        val deviceUUID = preferences[PreferencesKeys.DEVICE_UUID] ?: ""
        val lastForceFullSync = preferences[PreferencesKeys.LAST_FORCE_FULL_SYNC] ?: 0L
        val darkTheme = DarkThemeMode.valueOf(
            preferences[PreferencesKeys.DARK_THEME] ?: DarkThemeMode.SYSTEM.name
        )
        val cachedSupportNome = preferences[PreferencesKeys.SUPPORT_NOME] ?: ""
        val cachedSupportSuporte = preferences[PreferencesKeys.SUPPORT_SUPORTE] ?: ""
        val uniqueId = preferences[PreferencesKeys.UNIQUE_ID] ?: ""
        val idSync = preferences[PreferencesKeys.ID_SYNC] ?: ""
        val idCompany = preferences[PreferencesKeys.ID_COMPANY] ?: ""
        val companyName = preferences[PreferencesKeys.COMPANY_NAME] ?: ""
        val timeSync = (preferences[PreferencesKeys.TIME_SYNC] ?: "0").toInt()
        val timeAlive = (preferences[PreferencesKeys.TIME_ALIVE] ?: "0").toInt()

        return UserPreferences(
            acceptedOrDeclinedLocPerm = acceptedOrDeclinedLocPerm,
            deviceUUID = deviceUUID,
            lastForceFullSync = lastForceFullSync,
            theme = ThemeState(darkTheme),
            cachedSupportNome = cachedSupportNome,
            cachedSupportSuporte = cachedSupportSuporte,
            uniqueId = uniqueId,
            idSync = idSync,
            idCompany = idCompany,
            companyName = companyName,
            timeSync = timeSync,
            timeAlive = timeAlive
        )
    }
}