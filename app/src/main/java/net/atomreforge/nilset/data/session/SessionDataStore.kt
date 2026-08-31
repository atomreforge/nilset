package net.atomreforge.nilset.data.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 会话持久化：负责把 [SessionState] 读写到 DataStore。
 * 独立于 Repository，单一职责：只管「存」和「取」，不做业务判断。
 */
@Singleton
class SessionDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {

    suspend fun save(state: SessionState) {
        dataStore.edit { prefs ->
            prefs[IS_LOGGED_IN] = state.isLoggedIn
            prefs[IS_SPECIAL_MODE] = state.isSpecialMode
            state.accessToken?.let { prefs[ACCESS_TOKEN] = it }
            state.refreshToken?.let { prefs[REFRESH_TOKEN] = it }
            state.userInfo?.let { info ->
                prefs[USER_UID] = info.uid
                prefs[USER_USERNAME] = info.username
                prefs[USER_NICKNAME] = info.nickname
                info.email?.let { prefs[USER_EMAIL] = it }
                info.registerTime?.let { prefs[USER_REGISTER_TIME] = it }
                prefs[USER_ROLE] = info.role
            }
        }
    }

    suspend fun load(): SessionState? {
        val prefs = dataStore.data.first()
        val accessToken = prefs[ACCESS_TOKEN] ?: return null
        val refreshToken = prefs[REFRESH_TOKEN] ?: return null

        val userInfo = if (prefs[USER_UID] != null) {
            UserInfo(
                uid = prefs[USER_UID] ?: 0L,
                username = prefs[USER_USERNAME] ?: "",
                nickname = prefs[USER_NICKNAME] ?: "",
                email = prefs[USER_EMAIL],
                registerTime = prefs[USER_REGISTER_TIME],
                role = prefs[USER_ROLE] ?: "user",
            )
        } else null

        return SessionState(
            isLoggedIn = prefs[IS_LOGGED_IN] ?: true,
            isSpecialMode = prefs[IS_SPECIAL_MODE] ?: false,
            accessToken = accessToken,
            refreshToken = refreshToken,
            userInfo = userInfo,
        )
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    companion object {
        private val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        private val IS_SPECIAL_MODE = booleanPreferencesKey("is_special_mode")
        private val ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val USER_UID = longPreferencesKey("user_uid")
        private val USER_USERNAME = stringPreferencesKey("user_username")
        private val USER_NICKNAME = stringPreferencesKey("user_nickname")
        private val USER_EMAIL = stringPreferencesKey("user_email")
        private val USER_REGISTER_TIME = stringPreferencesKey("user_register_time")
        private val USER_ROLE = stringPreferencesKey("user_role")
    }
}
