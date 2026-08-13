package com.example.gittracker.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsManager @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private object Keys {
        val TRACK_SELF = booleanPreferencesKey("track_self")
    }

    val isTrackingSelf: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[Keys.TRACK_SELF] ?: false
    }

    suspend fun setTrackingSelf(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.TRACK_SELF] = enabled
        }
    }

    suspend fun addDownloadMapping(downloadId: Long, repoId: Long) {
        dataStore.edit { preferences ->
            preferences[longPreferencesKey("download_$downloadId")] = repoId
        }
    }

    suspend fun getRepoIdForDownload(downloadId: Long): Long? {
        return dataStore.data.first()[longPreferencesKey("download_$downloadId")]
    }

    suspend fun removeDownloadMapping(downloadId: Long) {
        dataStore.edit { preferences ->
            preferences.remove(longPreferencesKey("download_$downloadId"))
        }
    }
}
