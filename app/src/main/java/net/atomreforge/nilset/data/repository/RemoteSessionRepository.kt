package net.atomreforge.nilset.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import retrofit2.HttpException
import kotlinx.serialization.SerializationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.atomreforge.nilset.data.remote.api.DaizyNightApi
import net.atomreforge.nilset.data.remote.dto.SignOutRequest
import net.atomreforge.nilset.data.remote.dto.RefreshTokenRequest
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
    @param:SessionScope private val scope: CoroutineScope,
) : SessionRepository, SessionTokenRefresher {

    private val _sessionState = MutableStateFlow(SessionState())
    override val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()
    private val _isSessionReady = MutableStateFlow(false)
    override val isSessionReady: StateFlow<Boolean> = _isSessionReady.asStateFlow()
    private val refreshMutex = Mutex()

    init {
        scope.launch {
            try {
                restoreFromDisk()
            } finally {
                _isSessionReady.value = true
            }
        }
    }

    override suspend fun login(username: String, password: String): Result<Unit> {
        return try {
            val response = api.login(LoginRequest(username = username, password = password))
            val state = SessionState(
                isLoggedIn = true,
                username = username,
                accessToken = response.accessToken,
                refreshToken = response.refreshToken,
            )
            _sessionState.value = state
            persist(state)
            Result.success(Unit)
        } catch (e: Exception) {
            _sessionState.value = SessionState()
            dataStore.clear()
            Result.failure(toLoginMessage(e))
        }
    }

    override suspend fun enterLocalSession(username: String) {
        val state = SessionState(
            isLoggedIn = true,
            username = username,
            userInfo = UserInfo(
                uid = 0L,
                username = username,
                nickname = username,
                role = "local",
            ),
        )
        _sessionState.value = state
        persist(state)
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
            val username = _sessionState.value.username
                ?: _sessionState.value.userInfo?.username
            if (username.isNullOrBlank()) {
                return Result.failure(IllegalStateException("无法确定当前用户名"))
            }
            val response = api.getUserMe(username)
            _sessionState.update { it.copy(username = response.username) }
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

    override suspend fun logout(): Result<Unit> {
        val current = _sessionState.value
        if (current.isLoggedIn && current.accessToken != null) {
            runCatching { refreshAccessToken() }
            val latest = _sessionState.value
            val refreshToken = latest.refreshToken
            if (latest.isLoggedIn && latest.accessToken != null && refreshToken != null) {
                try {
                    api.signOut(SignOutRequest(refreshToken = refreshToken))
                } catch (_: Exception) {
                }
            }
        }
        _sessionState.value = SessionState()
        dataStore.clear()
        return Result.success(Unit)
    }

    override fun enterSpecialMode() {
        _sessionState.update {
            it.copy(
                isSpecialMode = true,
                isLoggedIn = false,
                username = null,
                userInfo = null,
                accessToken = null,
                refreshToken = null,
            )
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

    override fun refreshAccessTokenBlocking(): String? = runBlocking {
        refreshAccessToken()
    }

    private suspend fun refreshAccessToken(): String? = refreshMutex.withLock {
        val current = _sessionState.value
        val refreshToken = current.refreshToken
        if (!current.isLoggedIn || current.accessToken == null || refreshToken == null) {
            return@withLock null
        }

        val response = try {
            api.refreshAccessToken(RefreshTokenRequest(refreshToken = refreshToken))
        } catch (e: HttpException) {
            if (e.code() == 400 || e.code() == 401 || e.code() in 500..599) {
                clearSession()
            }
            return@withLock null
        } catch (_: IOException) {
            return@withLock null
        } catch (_: SerializationException) {
            return@withLock null
        }

        _sessionState.update {
            it.copy(
                accessToken = response.accessToken,
                refreshToken = response.refreshToken,
            )
        }
        persist(_sessionState.value)
        response.accessToken
    }

    private suspend fun clearSession() {
        _sessionState.value = SessionState()
        dataStore.clear()
    }

    private fun toLoginMessage(e: Exception): Exception {
        val message = when (e) {
            is HttpException -> when (e.code()) {
                400 -> "用户名或密码错误"
                401 -> "登录状态无效，请重新登录"
                403 -> "没有权限执行登录"
                429 -> "请求太频繁，请稍后再试"
                in 500..599 -> "服务端异常，请稍后再试"
                else -> "登录失败（${e.code()}）"
            }
            is IOException -> "无法连接服务端，请检查网络或后端地址"
            is SerializationException -> "服务端返回数据异常"
            else -> "登录请求失败"
        }
        return IllegalStateException(message, e)
    }
}
