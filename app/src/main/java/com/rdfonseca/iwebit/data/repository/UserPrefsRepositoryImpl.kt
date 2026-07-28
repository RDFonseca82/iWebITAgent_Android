package com.rdfonseca.iwebit.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.rdfonseca.iwebit.common.logAndWrite
import com.rdfonseca.iwebit.data.identity.CriticalIdentityStore
import com.rdfonseca.iwebit.data.identity.CriticalRegistration
import com.rdfonseca.iwebit.data.identity.DeviceIdentity
import com.rdfonseca.iwebit.data.remote.IWebITApi
import com.rdfonseca.iwebit.domain.model.DarkThemeMode
import com.rdfonseca.iwebit.domain.model.ThemeState
import com.rdfonseca.iwebit.domain.model.UserPreferences
import com.rdfonseca.iwebit.domain.repository.UserPrefsRepository
import java.io.IOException
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class UserPrefsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val context: Context,
    private val api: IWebITApi
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

    private val criticalIdentityStore = CriticalIdentityStore(context)
    private val identityMutex = Mutex()
    private val stableDeviceId: String by lazy { DeviceIdentity.stableDeviceId(context) }
    private val permanentUniqueId: String by lazy {
        DeviceIdentity.permanentUniqueId(stableDeviceId)
    }

    override val userPreferencesFlow: Flow<UserPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                logAndWrite(
                    context,
                    "ERROR: $exception. A recuperar dados críticos.",
                    "UserPrefsRepositoryImpl.kt",
                    "userPreferencesFlow"
                )
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map(::mapUserPreferences)

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
        val registration = if (idSync.isNullOrBlank()) {
            CriticalRegistration()
        } else {
            CriticalRegistration(
                idSync = idSync,
                idCompany = idCompany.orEmpty(),
                companyName = companyName.orEmpty(),
                timeSync = timeSync ?: "0",
                timeAlive = timeAlive ?: "0"
            )
        }

        val backup = criticalIdentityStore.read()
        var resolvedUniqueId = permanentUniqueId
        dataStore.edit { preferences ->
            val requestedUniqueId = uniqueId
                ?.takeIf(DeviceIdentity::isValidUniqueId)
                ?: permanentUniqueId
            resolvedUniqueId = DeviceIdentity.preferredUniqueId(
                storedUniqueId = preferences[PreferencesKeys.UNIQUE_ID],
                backedUpUniqueId = backup.uniqueId,
                generatedUniqueId = requestedUniqueId
            )
            preferences[PreferencesKeys.DEVICE_UUID] = stableDeviceId
            preferences[PreferencesKeys.UNIQUE_ID] = resolvedUniqueId
            preferences[PreferencesKeys.ID_SYNC] = registration.idSync
            preferences[PreferencesKeys.ID_COMPANY] = registration.idCompany
            preferences[PreferencesKeys.COMPANY_NAME] = registration.companyName
            preferences[PreferencesKeys.TIME_SYNC] = registration.timeSync
            preferences[PreferencesKeys.TIME_ALIVE] = registration.timeAlive
        }

        val persistedRegistration = registration.copy(uniqueId = resolvedUniqueId)
        if (registration.idSync.isBlank()) {
            criticalIdentityStore.write(persistedRegistration.copy(recoveryDisabled = true))
        } else {
            criticalIdentityStore.write(persistedRegistration)
        }
    }

    override suspend fun updateDeviceUUID() {
        identityMutex.withLock {
            restoreLocalIdentity()
        }
    }

    override suspend fun ensurePermanentIdentity() {
        identityMutex.withLock {
            restoreLocalIdentity()
            val current = userPreferencesFlow.first()
            if (current.idSync.isNotBlank()) return
            if (criticalIdentityStore.read().recoveryDisabled) return

            runCatching {
                val device = api.getDeviceInfo(current.uniqueId)
                require(device.UniqueID.equals(current.uniqueId, ignoreCase = true)) {
                    "A plataforma devolveu uma identidade diferente"
                }
                require(device.IDSync.isNotBlank()) {
                    "A plataforma não devolveu um IDSync"
                }
                val company = api.getCompanyInfo(device.IDSync)
                updateUserInfo(
                    uniqueId = current.uniqueId,
                    idSync = device.IDSync,
                    idCompany = company.IdCompany,
                    companyName = company.Company,
                    timeSync = company.TimeSync.toString(),
                    timeAlive = company.TimeAlive.toString()
                )
                logAndWrite(
                    context,
                    "Associação recuperada automaticamente para ${device.IDSync}",
                    "UserPrefsRepositoryImpl.kt",
                    "ensurePermanentIdentity"
                )
            }.onFailure { exception ->
                logAndWrite(
                    context,
                    "Recuperação automática indisponível: ${exception.message}",
                    "UserPrefsRepositoryImpl.kt",
                    "ensurePermanentIdentity"
                )
            }
        }
    }

    private suspend fun restoreLocalIdentity() {
        val backup = criticalIdentityStore.read()
        dataStore.edit { preferences ->
            val uniqueId = DeviceIdentity.preferredUniqueId(
                storedUniqueId = preferences[PreferencesKeys.UNIQUE_ID],
                backedUpUniqueId = backup.uniqueId,
                generatedUniqueId = permanentUniqueId
            )
            val idSync = preferences[PreferencesKeys.ID_SYNC]
                .orEmpty()
                .ifBlank { backup.idSync }
            val idCompany = preferences[PreferencesKeys.ID_COMPANY]
                .orEmpty()
                .ifBlank { backup.idCompany }
            val companyName = preferences[PreferencesKeys.COMPANY_NAME]
                .orEmpty()
                .ifBlank { backup.companyName }
            val timeSync = preferences[PreferencesKeys.TIME_SYNC]
                .orEmpty()
                .ifBlank { backup.timeSync }
            val timeAlive = preferences[PreferencesKeys.TIME_ALIVE]
                .orEmpty()
                .ifBlank { backup.timeAlive }

            preferences[PreferencesKeys.DEVICE_UUID] = stableDeviceId
            preferences[PreferencesKeys.UNIQUE_ID] = uniqueId
            preferences[PreferencesKeys.ID_SYNC] = idSync
            preferences[PreferencesKeys.ID_COMPANY] = idCompany
            preferences[PreferencesKeys.COMPANY_NAME] = companyName
            preferences[PreferencesKeys.TIME_SYNC] = timeSync
            preferences[PreferencesKeys.TIME_ALIVE] = timeAlive

            if (idSync.isNotBlank()) {
                criticalIdentityStore.write(
                    CriticalRegistration(
                        uniqueId = uniqueId,
                        idSync = idSync,
                        idCompany = idCompany,
                        companyName = companyName,
                        timeSync = timeSync,
                        timeAlive = timeAlive
                    )
                )
            }
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
        val backup = criticalIdentityStore.read()
        val acceptedOrDeclinedLocPerm = preferences[PreferencesKeys.YES_OR_NO_LOC_PERM] ?: false
        val lastForceFullSync = preferences[PreferencesKeys.LAST_FORCE_FULL_SYNC] ?: 0L
        val darkTheme = runCatching {
            DarkThemeMode.valueOf(
                preferences[PreferencesKeys.DARK_THEME] ?: DarkThemeMode.SYSTEM.name
            )
        }.getOrDefault(DarkThemeMode.SYSTEM)
        val cachedSupportNome = preferences[PreferencesKeys.SUPPORT_NOME] ?: ""
        val cachedSupportSuporte = preferences[PreferencesKeys.SUPPORT_SUPORTE] ?: ""
        val uniqueId = DeviceIdentity.preferredUniqueId(
            storedUniqueId = preferences[PreferencesKeys.UNIQUE_ID],
            backedUpUniqueId = backup.uniqueId,
            generatedUniqueId = permanentUniqueId
        )
        val idSync = preferences[PreferencesKeys.ID_SYNC]
            .orEmpty()
            .ifBlank { backup.idSync }
        val idCompany = preferences[PreferencesKeys.ID_COMPANY]
            .orEmpty()
            .ifBlank { backup.idCompany }
        val companyName = preferences[PreferencesKeys.COMPANY_NAME]
            .orEmpty()
            .ifBlank { backup.companyName }
        val timeSync = preferences[PreferencesKeys.TIME_SYNC]
            .orEmpty()
            .ifBlank { backup.timeSync }
            .toIntOrNull() ?: 0
        val timeAlive = preferences[PreferencesKeys.TIME_ALIVE]
            .orEmpty()
            .ifBlank { backup.timeAlive }
            .toIntOrNull() ?: 0

        return UserPreferences(
            acceptedOrDeclinedLocPerm = acceptedOrDeclinedLocPerm,
            deviceUUID = stableDeviceId,
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