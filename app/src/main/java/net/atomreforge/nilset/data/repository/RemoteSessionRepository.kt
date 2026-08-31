package net.atomreforge.nilset.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import net.atomreforge.nilset.data.remote.api.DaizyNightApi
import net.atomreforge.nilset.data.remote.dto.LoginRequest
import net.atomreforge.nilset.data.remote.dto.RegisterRequest
import net.atomreforge.nilset.data.session.SessionDataStore
import net.atomreforge.nilset.data.session.SessionState
import net.atomreforge.nilset.data.session.UserInfo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteSessionRepository @Inject constructor(
    private val api: DaizyNightApi,
    private val dataStore: SessionDataStore,
) : SessionRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _sessionState = MutableStateFlow(SessionState())
    override val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    init {
        scope.launch { restoreFromDisk() }
    }

    override suspend fun login(username: String, password: String): Result<Unit> {
        return try {
            val response = api.login(LoginRequest(username = username, password = password))
            _sessionState.update {
                it.copy(
                    isLoggedIn = true,
                    isSpecialMode = false,
                    accessToken = response.accessToken,
                    refreshToken = response.refreshToken,
                )
            }
            persist(_sessionState.value)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun register(
        username: String,
        nickname: String,
        password: String,
        registerCode: String,
    ): Result<Unit> {
        return try {
            api.register(
                RegisterRequest(
                    username = username,
                    nickname = nickname,
                    password = password,
                    registerCode = registerCode,
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun fetchUserInfo(): Result<UserInfo> {
        return try {
            val response = api.getUserMe()
            val info = UserInfo(
                uid = response.uid,
                username = response.username,
                nickname = response.nickname,
                email = response.email,
                registerTime = response.registerTime,
                role = response.role,
            )
            _sessionState.update { it.copy(userInfo = info) }
            persist(_sessionState.value)
            Result.success(info)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun logout() {
        _sessionState.value = SessionState()
        scope.launch { dataStore.clear() }
    }

    override fun enterSpecialMode() {
        _sessionState.update {
            it.copy(isSpecialMode = true, isLoggedIn = false, accessToken = null, refreshToken = null)
        }
        scope.launch { dataStore.clear() }
    }

    override fun clear() {
        _sessionState.value = SessionState()
        scope.launch { dataStore.clear() }
    }

    private suspend fun persist(state: SessionState) {
        dataStore.save(state)
    }

    private suspend fun restoreFromDisk() {
        val saved = dataStore.load() ?: return
        _sessionState.value = saved
    }
}
