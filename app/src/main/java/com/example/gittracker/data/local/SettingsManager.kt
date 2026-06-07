package com.example.gittracker.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val syncFrequencyKey = intPreferencesKey("sync_frequency")

    val syncFrequency: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[syncFrequencyKey] ?: 8 // Default 8 hours
    }

    suspend fun setSyncFrequency(hours: Int) {
        context.dataStore.edit { preferences ->
            preferences[syncFrequencyKey] = hours
        }
    }
}
