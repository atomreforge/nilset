package net.atomreforge.nilset.bili.download

internal object BiliVideoFileName {

    fun create(title: String?, bvid: String, qualityLabel: String): String {
        val safeTitle = title.orEmpty()
            .replace(INVALID_PATH_CHARS, "_")
            .replace("\"", "”")
            .trim()
            .ifBlank { bvid.ifBlank { "video" } }
        val safeBvid = bvid.ifBlank { "video" }
        val safeQualityLabel = qualityLabel.ifBlank { "unknown" }
        return "\"$safeTitle\"|$safeBvid|$safeQualityLabel.mp4"
    }

    private val INVALID_PATH_CHARS = Regex("[\\\\/\u0000]")
}
