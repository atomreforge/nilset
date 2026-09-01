package net.atomreforge.nilset.data.remote.interceptor

import dagger.Lazy
import net.atomreforge.nilset.const.ApiExpressions
import net.atomreforge.nilset.data.repository.SessionRepository
import net.atomreforge.nilset.data.repository.SessionTokenRefresher
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenAuthenticator @Inject constructor(
    private val sessionRepository: Lazy<SessionRepository>,
    private val tokenRefresher: Lazy<SessionTokenRefresher>,
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        val request = response.request
        if (request.header(ApiExpressions.Header.AUTHORIZATION) == null) return null
        if (request.url.encodedPath.endsWith("/refresh-access-token")) return null
        if (response.priorResponse != null) return null

        val accessToken = tokenRefresher.get().refreshAccessTokenBlocking()
            ?: return null

        return request.newBuilder()
            .header(
                ApiExpressions.Header.AUTHORIZATION,
                "${ApiExpressions.Header.BEARER_PREFIX} $accessToken",
            )
            .build()
    }
}
