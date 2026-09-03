package net.atomreforge.nilset.data.remote.interceptor

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import net.atomreforge.nilset.data.session.SessionState
import okhttp3.Authenticator
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class TokenAuthenticatorTest {
    @get:Rule
    val server = MockWebServer()

    @Test
    fun `does not retry request without authorization`() {
        server.enqueue(MockResponse().setResponseCode(401))
        val refresher = FakeTokenRefresher(NEW_ACCESS_TOKEN)
        val client = OkHttpClient.Builder()
            .authenticator(authenticator(refresher))
            .build()

        val response = client.newCall(request("/api/v1/user/alice/me")).execute()

        assertEquals(401, response.code)
        assertEquals(0, refresher.callCount)
    }

    @Test
    fun `does not retry refresh endpoint`() {
        server.enqueue(MockResponse().setResponseCode(401))
        val refresher = FakeTokenRefresher(NEW_ACCESS_TOKEN)
        val client = OkHttpClient.Builder()
            .authenticator(authenticator(refresher))
            .build()

        val response = client.newCall(request("/api/v1/refresh-access-token")).execute()

        assertEquals(401, response.code)
        assertEquals(0, refresher.callCount)
    }

    @Test
    fun `retries protected request with refreshed access token`() {
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setBody("{}"))
        val refresher = FakeTokenRefresher(NEW_ACCESS_TOKEN)
        val client = OkHttpClient.Builder()
            .authenticator(authenticator(refresher))
            .build()

        val response = client.newCall(request("/api/v1/user/alice/me", "old-access-token")).execute()

        assertEquals(200, response.code)
        assertEquals(1, refresher.callCount)
        assertEquals("Bearer old-access-token", server.takeRequest().headers.get("Authorization"))
        assertEquals("Bearer $NEW_ACCESS_TOKEN", server.takeRequest().headers.get("Authorization"))
    }

    @Test
    fun `does not retry when refresh fails`() {
        server.enqueue(MockResponse().setResponseCode(401))
        val refresher = FakeTokenRefresher(null)
        val client = OkHttpClient.Builder()
            .authenticator(authenticator(refresher))
            .build()

        val response = client.newCall(request("/api/v1/user/alice/me", "old-access-token")).execute()

        assertEquals(401, response.code)
        assertEquals(1, refresher.callCount)
    }

    private fun authenticator(tokenRefresher: FakeTokenRefresher): Authenticator {
        val repository = FakeSessionRepository(
            SessionState(isLoggedIn = true, accessToken = "old-access-token"),
        )
        return TokenAuthenticator(FakeLazy(repository), FakeLazy(tokenRefresher))
    }

    private fun request(path: String, accessToken: String? = null): Request {
        val builder = Request.Builder().url(server.url(path))
        accessToken?.let { builder.header("Authorization", "Bearer $it") }
        return builder.build()
    }

    private companion object {
        private const val NEW_ACCESS_TOKEN = "new-access-token"
    }
}
