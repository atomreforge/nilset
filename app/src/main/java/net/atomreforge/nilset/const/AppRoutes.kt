package net.atomreforge.nilset.const

import android.net.Uri

object AppRoutes {
    const val LOGIN = "login"
    const val CONSOLE = "console"
    const val MAIN = "main"
    const val THEME_SETTINGS = "theme_settings"
    const val BACKGROUND_CROP = "background_crop/{sourceUri}"

    fun backgroundCrop(sourceUri: String): String {
        return "background_crop/${Uri.encode(sourceUri)}"
    }

    object Tab {
        const val HOME = "home"
        const val SETTINGS = "settings"
    }
}
