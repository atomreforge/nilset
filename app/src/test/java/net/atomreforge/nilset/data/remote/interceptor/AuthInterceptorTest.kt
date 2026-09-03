package net.atomreforge.nilset.data.remote.interceptor

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import net.atomreforge.nilset.data.session.SessionState
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class AuthInterceptorTest {
    @get:Rule
    val server = MockWebServer()

    @Test
    fun `adds bearer token when session is authenticated`() {
        server.enqueue(MockResponse().setBody("{}"))
        val client = clientWithSession(loggedIn = true)

        client.newCall(request()).execute().use { response ->
            assertEquals(200, response.code)
        }

        assertEquals("Bearer access-token", server.takeRequest().headers.get("Authorization"))
    }

    @Test
    fun `does not add authorization without access token`() {
        server.enqueue(MockResponse().setBody("{}"))
        val client = clientWithSession(loggedIn = false)

        client.newCall(request()).execute().use { response ->
            assertEquals(200, response.code)
        }

        assertNull(server.takeRequest().headers.get("Authorization"))
    }

    private fun request(): Request = Request.Builder()
        .url(server.url("/api/v1/user/alice/me"))
        .build()

    private fun clientWithSession(loggedIn: Boolean): OkHttpClient {
        val repository = FakeSessionRepository(
            if (loggedIn) {
                SessionState(isLoggedIn = true, accessToken = "access-token")
            } else {
                SessionState()
            },
        )
        return OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(FakeLazy(repository)))
            .build()
    }
}
