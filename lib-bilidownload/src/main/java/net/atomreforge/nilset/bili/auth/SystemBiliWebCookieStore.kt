package net.atomreforge.nilset.bili.auth

import android.webkit.CookieManager

class SystemBiliWebCookieStore : BiliWebCookieStore {

    override fun flush() {
        CookieManager.getInstance().flush()
    }

    override fun readCookie(url: String): String? {
        return CookieManager.getInstance().getCookie(url)
    }

    override fun expireCookie(url: String, name: String) {
        CookieManager.getInstance().setCookie(
            url,
            "$name=; Max-Age=0; Path=/",
        )
    }
}
