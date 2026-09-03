package net.atomreforge.nilset.data.repository

import kotlinx.coroutines.test.runTest
import net.atomreforge.nilset.data.calendar.CalendarItem
import net.atomreforge.nilset.data.remote.api.DaizyNightApi
import net.atomreforge.nilset.data.remote.dto.CalendarItemResponse
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
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteCalendarRepositoryTest {

    @Test
    fun `get calendar maps response to client model`() = runTest {
        val records = listOf(
            CalendarItemResponse(weekday = 1, startMin = 480, endMin = 570, title = "数学"),
            CalendarItemResponse(weekday = 3, startMin = 840, endMin = 930, title = "体育"),
        )
        val api = FakeCalendarApi().apply {
            calendarResponse = CalendarResponse(calendarId = 7362514, records = records)
        }
        val repository = RemoteCalendarRepository(api)

        val result = repository.getCalendar("alice")

        assertEquals("alice", api.requestedUsernames.single())
        assertEquals(7362514, result.getOrThrow().calendarId)
        assertEquals(
            listOf(
                CalendarItem(weekday = 1, startMin = 480, endMin = 570, title = "数学"),
                CalendarItem(weekday = 3, startMin = 840, endMin = 930, title = "体育"),
            ),
            result.getOrThrow().records,
        )
    }

    @Test
    fun `save calendar maps records and succeeds`() = runTest {
        val api = FakeCalendarApi()
        val repository = RemoteCalendarRepository(api)
        val records = listOf(
            CalendarItem(weekday = 0, startMin = 600, endMin = 690, title = "自习"),
        )

        val result = repository.saveCalendar("alice", records)

        assertTrue(result.isSuccess)
        assertEquals(records, api.savedCalendar?.records?.map { response ->
            CalendarItem(response.weekday, response.startMin, response.endMin, response.title)
        })
    }

    @Test
    fun `delete calendar succeeds`() = runTest {
        val api = FakeCalendarApi()
        val repository = RemoteCalendarRepository(api)

        val result = repository.deleteCalendar("alice")

        assertTrue(result.isSuccess)
        assertEquals("alice", api.requestedUsernames.single())
    }

    @Test
    fun `repository propagates api failure as result failure`() = runTest {
        val repository = RemoteCalendarRepository(FakeCalendarApi(throwOnGet = true))

        val result = repository.getCalendar("alice")

        assertTrue(result.isFailure)
    }
}

private class FakeCalendarApi(
    private val throwOnGet: Boolean = false,
) : DaizyNightApi {
    val requestedUsernames = mutableListOf<String>()
    var calendarResponse = CalendarResponse(calendarId = 1, records = emptyList())
    var savedCalendar: CalendarPutRequest? = null

    override suspend fun register(body: RegisterRequest): RegisterResponse {
        throw UnsupportedOperationException()
    }

    override suspend fun login(body: LoginRequest): LoginResponse {
        throw UnsupportedOperationException()
    }

    override suspend fun refreshAccessToken(body: RefreshTokenRequest): LoginResponse {
        throw UnsupportedOperationException()
    }

    override suspend fun getUserMe(username: String): UserInfoResponse {
        throw UnsupportedOperationException()
    }

    override suspend fun getCalendar(username: String): CalendarResponse {
        requestedUsernames += username
        if (throwOnGet) throw IllegalStateException("calendar unavailable")
        return calendarResponse
    }

    override suspend fun putCalendar(
        username: String,
        body: CalendarPutRequest,
    ): MessageResponse {
        requestedUsernames += username
        savedCalendar = body
        return MessageResponse("ok")
    }

    override suspend fun deleteCalendar(username: String): MessageResponse {
        requestedUsernames += username
        return MessageResponse("ok")
    }

    override suspend fun signOut(body: SignOutRequest): MessageResponse {
        throw UnsupportedOperationException()
    }
}
