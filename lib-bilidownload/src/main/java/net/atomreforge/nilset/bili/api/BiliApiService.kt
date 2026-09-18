package net.atomreforge.nilset.bili.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import net.atomreforge.nilset.bili.model.BiliPlayUrlData
import net.atomreforge.nilset.bili.model.BiliVideoInfo
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

class BiliApiService(
    private val client: OkHttpClient,
    private val signer: BiliWbiSigner,
) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun resolveVideo(bvid: String): BiliVideoInfo = withContext(Dispatchers.IO) {
        val url = "https://api.bilibili.com/x/web-interface/view"
            .toHttpUrl()
            .newBuilder()
            .addQueryParameter("bvid", bvid)
            .build()
        val request = jsonRequest(url)
        executeJson(request) { wrapper ->
            json.decodeFromJsonElement(BiliVideoInfo.serializer(), wrapper.data ?: throw BiliApiException(-1, "data null"))
        }
    }

    suspend fun fetchPlayUrl(
        bvid: String,
        cid: Long,
        qn: Int = DEFAULT_QN,
    ): BiliPlayUrlData = withContext(Dispatchers.IO) {
        val signed = signer.sign(
            mapOf<String, String>(
                "bvid" to bvid,
                "cid" to cid.toString(),
                "qn" to qn.toString().toString(),
                "fnver" to "0",
                "fnval" to FNVAL_NO_AV1.toString(),
                "fourk" to "1",
            ),
        )
        val url = "https://api.bilibili.com/x/player/wbi/playurl"
            .toHttpUrl()
            .newBuilder()
            .apply {
                for ((key, value) in signed) addQueryParameter(key, value)
            }
            .build()
        val request = jsonRequest(url)
        executeJson(request) { wrapper ->
            json.decodeFromJsonElement(BiliPlayUrlData.serializer(), wrapper.data ?: throw BiliApiException(-1, "data null"))
        }
    }

    suspend fun probeContentLength(url: String): Long = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("Range", "bytes=0-0")
            .build()
        client.newCall(request).execute().use { response ->
            if (response.code == 206) {
                val contentRange = response.header("Content-Range")
                contentRange?.substringAfter('/')?.toLongOrNull() ?: -1
            } else -1
        }
    }

    private fun jsonRequest(url: okhttp3.HttpUrl): Request =
        Request.Builder()
            .url(url)
            .header("Accept", BiliHttpHeaders.ACCEPT_JSON)
            .build()

    private inline fun <T> executeJson(
        request: Request,
        block: (BiliApiEnvelope) -> T,
    ): T {
        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: throw BiliApiException(-1, "Empty response")
            if (!response.isSuccessful) throw BiliApiException(response.code, body)
            val wrapper = json.decodeFromString<BiliApiEnvelope>(body)
            if (wrapper.code != 0) throw BiliApiException(wrapper.code, wrapper.message ?: "Unknown error")
            val data = wrapper.data ?: throw BiliApiException(-1, "Upstream data is null")
            return block(wrapper)
        }
    }

    companion object {
        const val DEFAULT_QN = 0
        const val FNVAL_NO_AV1 = 2000
    }
}
