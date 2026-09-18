package net.atomreforge.nilset.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import android.content.Context
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import net.atomreforge.nilset.data.calendar.CalendarItem
import net.atomreforge.nilset.data.calendar.LocalCalendar
import net.atomreforge.nilset.data.calendar.UserCalendar
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

@Singleton
class PreferencesLocalCalendarRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val json: Json,
) : CalendarRepository {

    private val dataStore = context.scheduleDataStore

    override suspend fun getCalendar(username: String): Result<UserCalendar> = runCatching {
        val key = stringPreferencesKey(ScheduleStoreKeys.localCalendar(username))
        val serialized = dataStore.data.first()[key]
        if (serialized.isNullOrBlank()) {
            UserCalendar(calendarId = 0L, records = emptyList(), isInitialized = false)
        } else {
            val calendar = json.decodeFromString(LocalCalendar.serializer(), serialized)
            UserCalendar(
                calendarId = 0L,
                records = calendar.records.map { record ->
                    if (record.weekday == 7) {
                        record.copy(weekday = 0)
                    } else {
                        record
                    }
                },
            )
        }
    }

    override suspend fun saveCalendar(
        username: String,
        records: List<CalendarItem>,
    ): Result<Unit> = runCatching {
        val key = stringPreferencesKey(ScheduleStoreKeys.localCalendar(username))
        dataStore.edit { preferences ->
            preferences[key] = json.encodeToString(
                LocalCalendar.serializer(),
                LocalCalendar(records = records, isInitialized = true),
            )
        }
    }

    override suspend fun deleteCalendar(username: String): Result<Unit> = runCatching {
        val key = stringPreferencesKey(ScheduleStoreKeys.localCalendar(username))
        dataStore.edit { preferences ->
            preferences.remove(key)
        }
    }
}
