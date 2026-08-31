package net.atomreforge.nilset.data.remote.interceptor

import dagger.Lazy
import net.atomreforge.nilset.data.repository.SessionRepository
import okhttp3.Interceptor
import okhttp3.Response

/**
 * 统一为需要认证的请求附加 Authorization: Bearer <access_token>。
 * 通过 dagger.Lazy 延迟解析 SessionRepository，打破 OkHttpClient → AuthInterceptor → SessionRepository → Retrofit → OkHttpClient 的构建期循环依赖。
 */
class AuthInterceptor @javax.inject.Inject constructor(
    private val sessionRepository: Lazy<SessionRepository>,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = sessionRepository.get().sessionState.value.accessToken
        val request = if (token != null) {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}
