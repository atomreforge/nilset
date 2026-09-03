package net.atomreforge.nilset.data.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import net.atomreforge.nilset.data.remote.api.DaizyNightApi
import net.atomreforge.nilset.data.remote.dto.CalendarPutRequest
import net.atomreforge.nilset.data.remote.dto.CalendarResponse
import net.atomreforge.nilset.data.remote.dto.LoginRequest
import net.atomreforge.nilset.data.remote.dto.LoginResponse
import net.atomreforge.nilset.data.remote.dto.MessageResponse
import net.atomreforge.nilset.data.remote.dto.RefreshTokenRequest
import net.atomreforge.nilset.data.remote.dto.RegisterRequest
import net.atomreforge.nilset.data.remote.dto.RegisterResponse
import net.atomreforge.nilset.data.remote.dto.SignOutRequest
import net.atomreforge.nilset.data.remote.dto.UserInfoResponse
import net.atomreforge.nilset.data.session.SessionDataStore
import net.atomreforge.nilset.data.session.SessionState
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class RemoteSessionRepositoryTest {

    @Test
    fun `restores persisted session when repository becomes ready`() = runTest {
        val saved = loggedInState()
        val repository = createRepository(api = FakeDaizyNightApi(), dataStore = FakeSessionDataStore(saved))

        advanceUntilIdle()

        assertTrue(repository.isSessionReady.value)
        assertEquals(saved, repository.sessionState.value)
    }

    @Test
    fun `login success updates and persists session`() = runTest {
        val dataStore = FakeSessionDataStore()
        val api = FakeDaizyNightApi().apply {
            loginResponse = LoginResponse(ACCESS_TOKEN, REFRESH_TOKEN)
        }
        val repository = createRepository(api, dataStore)

        val result = repository.login("alice", "password")

        assertTrue(result.isSuccess)
        assertEquals("alice", repository.sessionState.value.username)
        assertEquals(ACCESS_TOKEN, repository.sessionState.value.accessToken)
        assertEquals(REFRESH_TOKEN, dataStore.saved?.refreshToken)
    }

    @Test
    fun `login failure clears current and persisted session`() = runTest {
        val dataStore = FakeSessionDataStore(loggedInState())
        val api = FakeDaizyNightApi().apply {
            loginError = IOException("offline")
        }
        val repository = createRepository(api, dataStore)

        val result = repository.login("alice", "password")
        advanceUntilIdle()

        assertTrue(result.isFailure)
        assertEquals("无法连接服务端，请检查网络或后端地址", result.exceptionOrNull()?.message)
        assertEquals(SessionState(), repository.sessionState.value)
        assertNull(dataStore.saved)
    }

    @Test
    fun `refresh success updates and persists both tokens`() = runTest {
        val dataStore = FakeSessionDataStore(loggedInState())
        val api = FakeDaizyNightApi().apply {
            refreshResponse = LoginResponse(NEW_ACCESS_TOKEN, NEW_REFRESH_TOKEN)
        }
        val repository = createRepository(api, dataStore)

        val token = repository.refreshAccessTokenBlocking()

        assertEquals(NEW_ACCESS_TOKEN, token)
        assertEquals(NEW_ACCESS_TOKEN, repository.sessionState.value.accessToken)
        assertEquals(NEW_REFRESH_TOKEN, repository.sessionState.value.refreshToken)
        assertEquals(NEW_REFRESH_TOKEN, dataStore.saved?.refreshToken)
    }

    @Test
    fun `invalid refresh clears session`() = runTest {
        val dataStore = FakeSessionDataStore(loggedInState())
        val api = FakeDaizyNightApi().apply {
            refreshError = httpException(401)
        }
        val repository = createRepository(api, dataStore)

        val token = repository.refreshAccessTokenBlocking()

        assertNull(token)
        assertEquals(SessionState(), repository.sessionState.value)
        assertNull(dataStore.saved)
    }

    @Test
    fun `network failure during refresh preserves session`() = runTest {
        val initial = loggedInState()
        val api = FakeDaizyNightApi().apply {
            refreshError = IOException("offline")
        }
        val repository = createRepository(api, FakeSessionDataStore(initial))

        val token = repository.refreshAccessTokenBlocking()

        assertNull(token)
        assertEquals(initial, repository.sessionState.value)
    }

    @Test
    fun `serialization failure during refresh preserves session`() = runTest {
        val initial = loggedInState()
        val api = FakeDaizyNightApi().apply {
            refreshError = SerializationException("invalid response")
        }
        val repository = createRepository(api, FakeSessionDataStore(initial))

        val token = repository.refreshAccessTokenBlocking()

        assertNull(token)
        assertEquals(initial, repository.sessionState.value)
    }

    @Test
    fun `logout signs out with refreshed token and clears local session`() = runTest {
        val dataStore = FakeSessionDataStore(loggedInState())
        val api = FakeDaizyNightApi().apply {
            refreshResponse = LoginResponse(NEW_ACCESS_TOKEN, NEW_REFRESH_TOKEN)
        }
        val repository = createRepository(api, dataStore)

        val result = repository.logout()

        assertTrue(result.isSuccess)
        assertEquals(NEW_REFRESH_TOKEN, api.signedOutRefreshTokens.single())
        assertEquals(SessionState(), repository.sessionState.value)
        assertNull(dataStore.saved)
    }

    private fun TestScope.createRepository(
        api: FakeDaizyNightApi,
        dataStore: FakeSessionDataStore,
    ): RemoteSessionRepository {
        return RemoteSessionRepository(api, dataStore, backgroundScope)
            .also { testScheduler.runCurrent() }
    }

    private fun loggedInState() = SessionState(
        isLoggedIn = true,
        username = "alice",
        accessToken = ACCESS_TOKEN,
        refreshToken = REFRESH_TOKEN,
    )

    private fun httpException(code: Int): HttpException =
        HttpException(Response.error<Any>(code, ByteArray(0).toResponseBody(null)))

    private companion object {
        private const val ACCESS_TOKEN = "access-1"
        private const val REFRESH_TOKEN = "refresh-1"
        private const val NEW_ACCESS_TOKEN = "access-2"
        private const val NEW_REFRESH_TOKEN = "refresh-2"
    }
}

private class FakeDaizyNightApi : DaizyNightApi {
    var loginResponse: LoginResponse? = null
    var loginError: Throwable? = null
    var refreshResponse: LoginResponse? = null
    var refreshError: Throwable? = null
    val signedOutRefreshTokens = mutableListOf<String>()

    override suspend fun register(body: RegisterRequest): RegisterResponse {
        throw UnsupportedOperationException()
    }

    override suspend fun login(body: LoginRequest): LoginResponse {
        loginError?.let { throw it }
        return loginResponse ?: throw IllegalStateException("login response is not configured")
    }

    override suspend fun refreshAccessToken(body: RefreshTokenRequest): LoginResponse {
        refreshError?.let { throw it }
        return refreshResponse ?: throw IllegalStateException("refresh response is not configured")
    }

    override suspend fun getUserMe(username: String): UserInfoResponse {
        throw UnsupportedOperationException()
    }

    override suspend fun getCalendar(username: String): CalendarResponse {
        throw UnsupportedOperationException()
    }

    override suspend fun putCalendar(
        username: String,
        body: CalendarPutRequest,
    ): MessageResponse {
        throw UnsupportedOperationException()
    }

    override suspend fun deleteCalendar(username: String): MessageResponse {
        throw UnsupportedOperationException()
    }

    override suspend fun signOut(body: SignOutRequest): MessageResponse {
        signedOutRefreshTokens += body.refreshToken
        return MessageResponse("signed out")
    }
}

private class FakeSessionDataStore(
    initial: SessionState? = null,
) : SessionDataStore {
    var saved: SessionState? = initial
        private set

    override suspend fun save(state: SessionState) {
        saved = state
    }

    override suspend fun load(): SessionState? = saved

    override suspend fun clear() {
        saved = null
    }
}
