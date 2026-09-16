package net.atomreforge.nilset.data.repository

import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import net.atomreforge.nilset.data.calendar.CalendarItem
import net.atomreforge.nilset.data.calendar.UserCalendar
import net.atomreforge.nilset.data.remote.ServerConnectionManager
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
import net.atomreforge.nilset.data.remote.interceptor.FakeSessionRepository
import net.atomreforge.nilset.data.session.SessionState
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LocalFirstCalendarSyncManagerTest {

    @Test
    fun `disconnected sync does not call remote calendar`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val localRepository = FakeLocalCalendarRepository(
            listOf(CalendarItem(weekday = 1, startMin = 480, endMin = 540, title = "数学")),
        )
        val remoteRepository = RecordingRemoteCalendarRepository(
            UserCalendar(calendarId = 1L, records = emptyList()),
        )
        val connectionManager = disconnectedConnectionManager(testScheduler)
        val syncManager = LocalFirstCalendarSyncManager(
            localCalendarRepository = localRepository,
            remoteCalendarRepository = remoteRepository,
            sessionRepository = FakeSessionRepository(
                SessionState(isLoggedIn = true, username = "alice"),
            ),
            serverConnectionManager = connectionManager,
            scope = CoroutineScope(SupervisorJob() + dispatcher),
        )
        advanceUntilIdle()

        val result = syncManager.syncIfConnected("alice")
        advanceUntilIdle()

        assertEquals(Result.success(Unit), result)
        assertEquals(0, remoteRepository.getCallCount)
        assertEquals(0, remoteRepository.saveCallCount)
    }

    @Test
    fun `connected sync overwrites different remote with local`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val localRecords = listOf(
            CalendarItem(
                weekday = 1,
                startMin = 480,
                endMin = 540,
                title = "数学",
                teacher = "张老师",
            ),
        )
        val localRepository = FakeLocalCalendarRepository(localRecords)
        val remoteRepository = RecordingRemoteCalendarRepository(
            UserCalendar(
                calendarId = 1L,
                records = listOf(
                    CalendarItem(weekday = 2, startMin = 480, endMin = 540, title = "体育"),
                ),
            ),
        )
        connectedSyncManager(
            testScheduler,
            localRepository,
            remoteRepository,
        )
        advanceUntilIdle()

        assertEquals("alice", remoteRepository.savedUsernames.single())
        assertEquals(localRecords, remoteRepository.savedRecords)
    }

    @Test
    fun `connected sync skips put when core records match`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val coreRecord = CalendarItem(weekday = 1, startMin = 480, endMin = 540, title = "数学")
        val localRepository = FakeLocalCalendarRepository(
            listOf(coreRecord.copy(teacher = "张老师")),
        )
        val remoteRepository = RecordingRemoteCalendarRepository(
            UserCalendar(calendarId = 1L, records = listOf(coreRecord)),
        )
        connectedSyncManager(
            testScheduler,
            localRepository,
            remoteRepository,
        )
        advanceUntilIdle()

        assertEquals(1, remoteRepository.getCallCount)
        assertEquals(0, remoteRepository.saveCallCount)
    }

    @Test
    fun `connection restored syncs local calendar automatically`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val localRepository = FakeLocalCalendarRepository(
            listOf(CalendarItem(weekday = 1, startMin = 480, endMin = 540, title = "数学")),
        )
        val remoteRepository = RecordingRemoteCalendarRepository(
            UserCalendar(calendarId = 1L, records = emptyList()),
        )
        val api = FakeHealthApi(error = IOException("offline"))
        val connectionManager = ServerConnectionManager(
            api = api,
            scope = CoroutineScope(SupervisorJob() + dispatcher),
            clock = schedulerClock(testScheduler),
        )
        connectionManager.startInitialCheckIfNeeded()
        advanceUntilIdle()
        val syncManager = LocalFirstCalendarSyncManager(
            localCalendarRepository = localRepository,
            remoteCalendarRepository = remoteRepository,
            sessionRepository = FakeSessionRepository(
                SessionState(isLoggedIn = true, username = "alice"),
            ),
            serverConnectionManager = connectionManager,
            scope = CoroutineScope(SupervisorJob() + dispatcher),
        )
        advanceUntilIdle()

        api.error = null
        api.response = MessageResponse("ok")
        advanceTimeBy(ServerConnectionManager.RETRY_COOLDOWN_MILLIS)
        connectionManager.requestManualRetry()
        advanceUntilIdle()

        assertEquals(1, remoteRepository.saveCallCount)
        assertEquals(localRepository.records, remoteRepository.savedRecords)
    }

    private fun connectedSyncManager(
        scheduler: TestCoroutineScheduler,
        localRepository: FakeLocalCalendarRepository,
        remoteRepository: RecordingRemoteCalendarRepository,
    ): LocalFirstCalendarSyncManager {
        val dispatcher = StandardTestDispatcher(scheduler)
        val connectionManager = ServerConnectionManager(
            api = FakeHealthApi(response = MessageResponse("ok")),
            scope = CoroutineScope(SupervisorJob() + dispatcher),
            clock = schedulerClock(scheduler),
        )
        connectionManager.startInitialCheckIfNeeded()
        return LocalFirstCalendarSyncManager(
            localCalendarRepository = localRepository,
            remoteCalendarRepository = remoteRepository,
            sessionRepository = FakeSessionRepository(
                SessionState(isLoggedIn = true, username = "alice"),
            ),
            serverConnectionManager = connectionManager,
            scope = CoroutineScope(SupervisorJob() + dispatcher),
        )
    }

    private fun disconnectedConnectionManager(
        scheduler: TestCoroutineScheduler,
    ): ServerConnectionManager {
        val dispatcher = StandardTestDispatcher(scheduler)
        val connectionManager = ServerConnectionManager(
            api = FakeHealthApi(error = IOException("offline")),
            scope = CoroutineScope(SupervisorJob() + dispatcher),
            clock = schedulerClock(scheduler),
        )
        connectionManager.startInitialCheckIfNeeded()
        return connectionManager
    }

    private fun schedulerClock(scheduler: TestCoroutineScheduler): Clock = object : Clock() {
        override fun getZone(): ZoneId = ZoneId.of("UTC")

        override fun withZone(zone: ZoneId): Clock = this

        override fun instant(): Instant = Instant.ofEpochMilli(scheduler.currentTime)
    }
}

private class FakeLocalCalendarRepository(
    val records: List<CalendarItem>,
) : CalendarRepository {

    override suspend fun getCalendar(username: String): Result<UserCalendar> = Result.success(
        UserCalendar(calendarId = 0L, records = records),
    )

    override suspend fun saveCalendar(
        username: String,
        records: List<CalendarItem>,
    ): Result<Unit> = Result.success(Unit)

    override suspend fun deleteCalendar(username: String): Result<Unit> = Result.success(Unit)
}

private class RecordingRemoteCalendarRepository(
    private val calendar: UserCalendar,
) : CalendarRepository {
    var getCallCount = 0
        private set
    var saveCallCount = 0
        private set
    val savedUsernames = mutableListOf<String>()
    val savedRecords = mutableListOf<CalendarItem>()

    override suspend fun getCalendar(username: String): Result<UserCalendar> {
        getCallCount++
        return Result.success(calendar)
    }

    override suspend fun saveCalendar(
        username: String,
        records: List<CalendarItem>,
    ): Result<Unit> {
        saveCallCount++
        savedUsernames.add(username)
        savedRecords.clear()
        savedRecords.addAll(records)
        return Result.success(Unit)
    }

    override suspend fun deleteCalendar(username: String): Result<Unit> = Result.success(Unit)
}

private class FakeHealthApi(
    var response: MessageResponse? = null,
    var error: Exception? = null,
) : DaizyNightApi {

    override suspend fun register(body: RegisterRequest): RegisterResponse =
        throw AssertionError("unexpected register call")

    override suspend fun login(body: LoginRequest): LoginResponse =
        throw AssertionError("unexpected login call")

    override suspend fun refreshAccessToken(body: RefreshTokenRequest): LoginResponse =
        throw AssertionError("unexpected refresh call")

    override suspend fun getUserInfo(username: String): UserInfoResponse =
        throw AssertionError("unexpected user call")

    override suspend fun getAnyCalendar(username: String): CalendarResponse =
        throw AssertionError("unexpected calendar get call")

    override suspend fun putCalendar(
        username: String,
        body: CalendarPutRequest,
    ): MessageResponse = throw AssertionError("unexpected calendar put call")

    override suspend fun healthDb(): MessageResponse {
        error?.let { throw it }
        return response ?: throw AssertionError("health response is not configured")
    }

    override suspend fun deleteCalendar(username: String): MessageResponse =
        throw AssertionError("unexpected calendar delete call")

    override suspend fun signOut(body: SignOutRequest): MessageResponse =
        throw AssertionError("unexpected sign out call")
}
