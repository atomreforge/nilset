package net.atomreforge.nilset.bili.api

import android.content.Context
import android.content.SharedPreferences
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

class BiliCookieStore(context: Context) : CookieJar {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val lock = Any()

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        synchronized(lock) {
            val all = prefs.all ?: return emptyList()
            return all.mapNotNull { entry ->
                    val encoded = entry.key
                    val raw = entry.value as? String ?: return@mapNotNull null
                runCatching {
                    val domain = decoded(encoded).substringBefore(DELIMITER)
                    if (!url.host.endsWith(domain, ignoreCase = true)) return@mapNotNull null
                    Cookie.Builder()
                        .name(decoded(raw).substringBefore("="))
                        .value(decoded(raw).substringAfter("="))
                        .domain(domain)
                        .build()
                }.getOrNull()
            }
        }
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        synchronized(lock) {
            prefs.edit().apply {
                for (cookie in cookies) {
                    val key = encoded(cookie.domain)
                    val value = encoded("${cookie.name}=${cookie.value}")
                    putString(key, value)
                }
            }.apply()
        }
    }

    fun clear() {
        synchronized(lock) { prefs.edit().clear().apply() }
    }

    private fun encoded(raw: String): String =
        java.util.Base64.getUrlEncoder().encodeToString(raw.toByteArray(Charsets.UTF_8))

    private fun decoded(encoded: String): String =
        String(java.util.Base64.getUrlDecoder().decode(encoded), Charsets.UTF_8)

    companion object {
        private const val PREFS_NAME = "bili_nil_cookies"
        private const val DELIMITER = "\u0000"
    }
}