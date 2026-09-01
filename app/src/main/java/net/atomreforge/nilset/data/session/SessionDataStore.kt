package net.atomreforge.nilset.data.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import net.atomreforge.nilset.const.SessionStoreKeys
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
            state.username?.let { prefs[USER_USERNAME] = it }
            state.accessToken?.let { prefs[ACCESS_TOKEN] = it }
            state.refreshToken?.let { prefs[REFRESH_TOKEN] = it }
            state.userInfo?.let { info ->
                prefs[USER_UID] = info.uid
                prefs[USER_NICKNAME] = info.nickname
                info.email?.let { prefs[USER_EMAIL] = it }
                info.registerTime?.let { prefs[USER_REGISTER_TIME] = it }
                prefs[USER_ROLE] = info.role
            }
            if (state.userInfo == null) {
                prefs.remove(USER_UID)
                prefs.remove(USER_NICKNAME)
                prefs.remove(USER_EMAIL)
                prefs.remove(USER_REGISTER_TIME)
                prefs.remove(USER_ROLE)
            }
            if (state.accessToken == null) {
                prefs.remove(ACCESS_TOKEN)
            }
            if (state.refreshToken == null) {
                prefs.remove(REFRESH_TOKEN)
            }
        }
    }

    suspend fun load(): SessionState? {
        val prefs = dataStore.data.first()
        if (prefs[IS_LOGGED_IN] != true && prefs[ACCESS_TOKEN] == null) return null
        val accessToken = prefs[ACCESS_TOKEN]
        val refreshToken = prefs[REFRESH_TOKEN]

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
            username = prefs[USER_USERNAME],
            accessToken = accessToken,
            refreshToken = refreshToken,
            userInfo = userInfo,
        )
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    companion object {
        private val IS_LOGGED_IN = booleanPreferencesKey(SessionStoreKeys.IS_LOGGED_IN)
        private val IS_SPECIAL_MODE = booleanPreferencesKey(SessionStoreKeys.IS_SPECIAL_MODE)
        private val ACCESS_TOKEN = stringPreferencesKey(SessionStoreKeys.ACCESS_TOKEN)
        private val REFRESH_TOKEN = stringPreferencesKey(SessionStoreKeys.REFRESH_TOKEN)
        private val USER_UID = longPreferencesKey(SessionStoreKeys.USER_UID)
        private val USER_USERNAME = stringPreferencesKey(SessionStoreKeys.USER_USERNAME)
        private val USER_NICKNAME = stringPreferencesKey(SessionStoreKeys.USER_NICKNAME)
        private val USER_EMAIL = stringPreferencesKey(SessionStoreKeys.USER_EMAIL)
        private val USER_REGISTER_TIME = stringPreferencesKey(SessionStoreKeys.USER_REGISTER_TIME)
        private val USER_ROLE = stringPreferencesKey(SessionStoreKeys.USER_ROLE)
    }
}
