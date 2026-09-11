package net.atomreforge.nilset.data.remote.interceptor

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import net.atomreforge.nilset.data.repository.ConfigRepository
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response

@Singleton
class DynamicBaseUrlInterceptor @Inject constructor(
    private val configRepository: ConfigRepository,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val baseUrl = runBlocking { configRepository.baseUrl.first() }
        val dynamicBaseUrl = requireNotNull(baseUrl.toHttpUrlOrNull()) {
            "Invalid dynamic server address: $baseUrl"
        }
        val dynamicRequest = request.newBuilder()
            .url(
                request.url.newBuilder()
                    .scheme(dynamicBaseUrl.scheme)
                    .host(dynamicBaseUrl.host)
                    .port(dynamicBaseUrl.port)
                    .build(),
            )
            .build()
        return chain.proceed(dynamicRequest)
    }
}
