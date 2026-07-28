package com.rdfonseca.iwebit.data.identity

import android.content.Context

data class CriticalRegistration(
    val uniqueId: String = "",
    val idSync: String = "",
    val idCompany: String = "",
    val companyName: String = "",
    val timeSync: String = "0",
    val timeAlive: String = "0",
    val recoveryDisabled: Boolean = false
)

class CriticalIdentityStore(context: Context) {
    private val preferences = context.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    fun read(): CriticalRegistration = CriticalRegistration(
        uniqueId = preferences.getString(KEY_UNIQUE_ID, "").orEmpty(),
        idSync = preferences.getString(KEY_ID_SYNC, "").orEmpty(),
        idCompany = preferences.getString(KEY_ID_COMPANY, "").orEmpty(),
        companyName = preferences.getString(KEY_COMPANY_NAME, "").orEmpty(),
        timeSync = preferences.getString(KEY_TIME_SYNC, "0") ?: "0",
        timeAlive = preferences.getString(KEY_TIME_ALIVE, "0") ?: "0",
        recoveryDisabled = preferences.getBoolean(KEY_RECOVERY_DISABLED, false)
    )

    fun write(registration: CriticalRegistration) {
        preferences.edit()
            .putString(KEY_UNIQUE_ID, registration.uniqueId)
            .putString(KEY_ID_SYNC, registration.idSync)
            .putString(KEY_ID_COMPANY, registration.idCompany)
            .putString(KEY_COMPANY_NAME, registration.companyName)
            .putString(KEY_TIME_SYNC, registration.timeSync)
            .putString(KEY_TIME_ALIVE, registration.timeAlive)
            .putBoolean(KEY_RECOVERY_DISABLED, registration.recoveryDisabled)
            .commit()
    }

    fun clear() = write(CriticalRegistration(recoveryDisabled = true))

    companion object {
        const val PREFERENCES_NAME = "critical_identity"
        private const val KEY_UNIQUE_ID = "unique_id"
        private const val KEY_ID_SYNC = "id_sync"
        private const val KEY_ID_COMPANY = "id_company"
        private const val KEY_COMPANY_NAME = "company_name"
        private const val KEY_TIME_SYNC = "time_sync"
        private const val KEY_TIME_ALIVE = "time_alive"
        private const val KEY_RECOVERY_DISABLED = "recovery_disabled"
    }
}