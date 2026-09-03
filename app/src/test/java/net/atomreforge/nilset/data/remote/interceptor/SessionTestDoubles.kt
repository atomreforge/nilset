package net.atomreforge.nilset.data.remote.interceptor

import dagger.Lazy
import kotlinx.coroutines.flow.MutableStateFlow
import net.atomreforge.nilset.data.repository.SessionRepository
import net.atomreforge.nilset.data.repository.SessionTokenRefresher
import net.atomreforge.nilset.data.session.SessionState
import net.atomreforge.nilset.data.session.UserInfo

internal class FakeLazy<T>(private val value: T) : Lazy<T> {
    override fun get(): T = value
}

internal class FakeTokenRefresher(private val token: String?) : SessionTokenRefresher {
    var callCount = 0
        private set

    override fun refreshAccessTokenBlocking(): String? {
        callCount++
        return token
    }
}

internal class FakeSessionRepository(initialState: SessionState) : SessionRepository {
    override val sessionState = MutableStateFlow(initialState)
    override val isSessionReady = MutableStateFlow(true)

    override suspend fun login(username: String, password: String): Result<Unit> =
        Result.failure(UnsupportedOperationException())

    override suspend fun enterLocalSession(username: String) =
        throw UnsupportedOperationException()

    override suspend fun register(
        username: String,
        nickname: String,
        password: String,
        registerCode: String,
    ): Result<Unit> = Result.failure(UnsupportedOperationException())

    override suspend fun fetchUserInfo(): Result<UserInfo> =
        Result.failure(UnsupportedOperationException())

    override suspend fun logout(): Result<Unit> = Result.failure(UnsupportedOperationException())

    override fun enterSpecialMode() = throw UnsupportedOperationException()

    override fun clear() = throw UnsupportedOperationException()
}
