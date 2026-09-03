package net.atomreforge.nilset.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import android.content.Context
import kotlinx.coroutines.flow.first
import net.atomreforge.nilset.const.ScheduleStoreKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private val Context.scheduleDataStore: DataStore<Preferences> by preferencesDataStore(
    name = ScheduleStoreKeys.STORE_NAME,
)

@Singleton
class PreferencesScheduleViewRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : ScheduleViewRepository {

    private val dataStore = context.scheduleDataStore

    override suspend fun lastViewedUsername(ownerUsername: String): String? =
        dataStore.data.first()[stringPreferencesKey(ScheduleStoreKeys.lastViewedUsername(ownerUsername))]

    override suspend fun setLastViewedUsername(
        ownerUsername: String,
        username: String,
    ) {
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey(ScheduleStoreKeys.lastViewedUsername(ownerUsername))] = username
        }
    }
}
