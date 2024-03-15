package com.rdfonseca.iwebit.di

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import com.rdfonseca.iwebit.common.Constants

val Context.dataStore by preferencesDataStore(
    corruptionHandler = ReplaceFileCorruptionHandler(
        produceNewData = { emptyPreferences() }
    ),
    name = Constants.USER_PREFERENCES_NAME
)
