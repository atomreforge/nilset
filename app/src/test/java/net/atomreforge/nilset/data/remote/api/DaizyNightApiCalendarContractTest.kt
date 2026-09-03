package net.atomreforge.nilset.data.remote.api

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import net.atomreforge.nilset.data.remote.dto.CalendarItemResponse
import net.atomreforge.nilset.data.remote.dto.CalendarPutRequest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class DaizyNightApiCalendarContractTest {
    @get:Rule
    val server = MockWebServer()

    private val json = Json { ignoreUnknownKeys = true }

    private val api: DaizyNightApi by lazy {
        Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(
                json.asConverterFactory("application/json".toMediaType()),
            )
            .build()
            .create(DaizyNightApi::class.java)
    }

    @Test
    fun `get calendar uses documented path and json contract`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"calendar_id":7362514,"records":[
                    {"weekday":1,"start_min":480,"end_min":570,"title":"数学"}
                ]}""",
            ),
        )

        val calendar = api.getCalendar("alice")

        assertEquals("/api/v1/user/alice/calendar", server.takeRequest().path)
        assertEquals(7362514, calendar.calendarId)
        assertEquals(1, calendar.records.single().weekday)
    }

    @Test
    fun `put calendar uses documented path and request json`() = runTest {
        server.enqueue(MockResponse().setBody("""{"message":"ok"}"""))

        val response = api.putCalendar(
            username = "alice",
            body = CalendarPutRequest(
                records = listOf(
                    CalendarItemResponse(weekday = 1, startMin = 480, endMin = 570, title = "数学"),
                ),
            ),
        )

        val request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/api/v1/user/alice/calendar", request.path)
        assertEquals(
            """{"records":[{"weekday":1,"start_min":480,"end_min":570,"title":"数学"}]}""",
            request.body.readUtf8(),
        )
        assertEquals("ok", response.message)
    }

    @Test
    fun `delete calendar uses documented path`() = runTest {
        server.enqueue(MockResponse().setBody("""{"message":"ok"}"""))

        val response = api.deleteCalendar("alice")

        val request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/api/v1/user/alice/calendar", request.path)
        assertEquals("ok", response.message)
    }
}
