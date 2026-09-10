package net.atomreforge.nilset.data.remote

import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ServerConnectionManagerTest {

    @Test
    fun `initial check connects once per process`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val api = FakeHealthApi(response = MessageResponse("ok"))
        val manager = ServerConnectionManager(
            api = api,
            scope = CoroutineScope(SupervisorJob() + dispatcher),
            clock = mutableClock(testScheduler),
        )

        manager.startInitialCheckIfNeeded()
        advanceUntilIdle()

        assertEquals(ServerConnectionStatus.CONNECTED, manager.uiState.value.status)
        assertEquals(1, api.healthCallCount)

        manager.startInitialCheckIfNeeded()
        advanceUntilIdle()

        assertEquals(1, api.healthCallCount)
    }

    @Test
    fun `failed check unlocks manual retry after cooldown`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val api = FakeHealthApi(error = IOException("offline"))
        val healthGate = CompletableDeferred<Unit>()
        api.healthGate = healthGate
        val manager = ServerConnectionManager(
            api = api,
            scope = CoroutineScope(SupervisorJob() + dispatcher),
            clock = mutableClock(testScheduler),
        )

        manager.startInitialCheckIfNeeded()
        runCurrent()

        assertEquals(ServerConnectionStatus.CHECKING, manager.uiState.value.status)
        assertFalse(manager.uiState.value.canRetry)

        advanceTimeBy(ServerConnectionManager.RETRY_COOLDOWN_MILLIS - 1)
        runCurrent()
        healthGate.complete(Unit)
        runCurrent()
        assertEquals(ServerConnectionStatus.DISCONNECTED, manager.uiState.value.status)
        assertFalse(manager.uiState.value.canRetry)

        advanceTimeBy(1)
        runCurrent()
        assertEquals(ServerConnectionStatus.DISCONNECTED, manager.uiState.value.status)
        assertTrue(manager.uiState.value.canRetry)
    }

    @Test
    fun `manual retry requests health again`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val clock = mutableClock(testScheduler)
        val api = FakeHealthApi(error = IOException("offline"))
        val manager = ServerConnectionManager(
            api = api,
            scope = CoroutineScope(SupervisorJob() + dispatcher),
            clock = clock,
        )

        manager.startInitialCheckIfNeeded()
        advanceUntilIdle()
        assertTrue(manager.uiState.value.canRetry)

        api.error = null
        api.response = MessageResponse("ok")
        manager.requestManualRetry()
        advanceUntilIdle()

        assertEquals(2, api.healthCallCount)
        assertEquals(ServerConnectionStatus.CONNECTED, manager.uiState.value.status)
    }

    private fun mutableClock(scheduler: TestCoroutineScheduler): Clock = object : Clock() {
        override fun getZone(): ZoneId = ZoneId.of("UTC")

        override fun withZone(zone: ZoneId): Clock = this

        override fun instant(): Instant = Instant.ofEpochMilli(scheduler.currentTime)
    }
}

private class FakeHealthApi(
    var response: MessageResponse? = null,
    var error: Exception? = null,
) : DaizyNightApi {
    var healthCallCount = 0
    var healthGate: CompletableDeferred<Unit>? = null

    override suspend fun register(body: RegisterRequest): RegisterResponse {
        throw AssertionError("unexpected register call")
    }

    override suspend fun login(body: LoginRequest): LoginResponse {
        throw AssertionError("unexpected login call")
    }

    override suspend fun refreshAccessToken(body: RefreshTokenRequest): LoginResponse {
        throw AssertionError("unexpected refresh call")
    }

    override suspend fun getUserMe(username: String): UserInfoResponse {
        throw AssertionError("unexpected user call")
    }

    override suspend fun getCalendar(username: String): CalendarResponse {
        throw AssertionError("unexpected calendar get call")
    }

    override suspend fun putCalendar(
        username: String,
        body: CalendarPutRequest,
    ): MessageResponse {
        throw AssertionError("unexpected calendar put call")
    }

    override suspend fun healthDb(): MessageResponse {
        healthCallCount++
        healthGate?.await()
        error?.let { throw it }
        return response ?: throw AssertionError("health response is not configured")
    }

    override suspend fun deleteCalendar(username: String): MessageResponse {
        throw AssertionError("unexpected calendar delete call")
    }

    override suspend fun signOut(body: SignOutRequest): MessageResponse {
        throw AssertionError("unexpected sign out call")
    }
}
