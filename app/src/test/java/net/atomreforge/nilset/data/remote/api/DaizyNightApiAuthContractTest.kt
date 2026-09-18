package net.atomreforge.nilset.data.remote.api

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import net.atomreforge.nilset.data.remote.dto.LoginRequest
import net.atomreforge.nilset.data.remote.dto.LoginResponse
import net.atomreforge.nilset.data.remote.dto.PublicUserInfoResponse
import net.atomreforge.nilset.data.remote.dto.RegisterRequest
import net.atomreforge.nilset.data.remote.dto.RegisterResponse
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class DaizyNightApiAuthContractTest {
    @get:Rule
    val server = MockWebServer()

    private val json = Json { ignoreUnknownKeys = true }

    private val api: DaizyNightApi by lazy {
        Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(DaizyNightApi::class.java)
    }

    @Test
    fun `register uses documented path and request json`() = runTest {
        server.enqueue(MockResponse().setBody("""{"message":"ok"}"""))
        val request = RegisterRequest(
            username = "alice",
            nickname = "Alice",
            password = "secret123",
            registerCode = "payload.signature",
        )

        val response = api.register(request)

        val recorded = server.takeRequest()
        assertEquals("/api/v1/register", recorded.path)
        assertEquals(
            """{"registerway":"legacy","username":"alice","nickname":"Alice",""" +
                """"password":"secret123","registercode":"payload.signature"}""",
            recorded.body.readUtf8(),
        )
        assertEquals("ok", response.message)
    }

    @Test
    fun `login uses documented path and request json`() = runTest {
        server.enqueue(
            MockResponse().setBody("""{"access_token":"access","refresh_token":"refresh"}"""),
        )

        val response = api.login(
            LoginRequest(username = "alice", password = "secret123"),
        )

        val recorded = server.takeRequest()
        assertEquals("/api/v1/login", recorded.path)
        assertEquals(
            """{"loginway":"legacy","username":"alice","password":"secret123","entrycode":""}""",
            recorded.body.readUtf8(),
        )
        assertEquals(LoginResponse("access", "refresh"), response)
    }

    @Test
    fun `public user info uses documented path and json contract`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"uid":1527277,"username":"alice","nickname":"Alice",""" +
                    """"register_time":"2026-01-02T03:04:05Z","role":"user"}""",
            ),
        )

        val response = api.getPublicUserInfo("alice")

        assertEquals("/api/v1/public/user/alice/info", server.takeRequest().path)
        assertEquals(
            PublicUserInfoResponse(
                uid = 1527277L,
                username = "alice",
                nickname = "Alice",
                registerTime = "2026-01-02T03:04:05Z",
                role = "user",
            ),
            response,
        )
    }
}
