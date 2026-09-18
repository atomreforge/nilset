package net.atomreforge.nilset.bili.download

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import net.atomreforge.nilset.bili.api.BiliApiException
import net.atomreforge.nilset.bili.api.BiliLogger
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.RandomAccessFile
import kotlin.random.Random

class BiliChunkDownloader(
    private val client: OkHttpClient,
) {

    suspend fun downloadToFile(
        url: String,
        destination: File,
        alreadyDownloaded: Long,
        totalExpected: Long,
        onProgress: (Long) -> Unit,
        alternateUrl: String? = null,
    ): Long = withContext(Dispatchers.IO) {
        var lastUrl = url
        var attempts = 0
        while (attempts <= MAX_RETRIES) {
            try {
                return@withContext attemptSinglePass(lastUrl, destination, alreadyDownloaded, onProgress)
            } catch (e: BiliApiException) {
                if (e.code == 403 && alternateUrl != null && lastUrl != alternateUrl) {
                    BiliLogger.w(TAG, "403 on primary, switching to alternate URL")
                    lastUrl = alternateUrl
                    continue
                }
                if (e.code == 403) throw LinkExpiredException(lastUrl, e)
                attempts++
                if (attempts > MAX_RETRIES) throw e
                delay(BASE_BACKOFF_MS * (1L shl attempts) + Random.nextLong(JITTER_MIN_MS, JITTER_MAX_MS))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                attempts++
                if (attempts > MAX_RETRIES) throw e
                delay(BASE_BACKOFF_MS * (1L shl attempts))
            }
        }
        throw BiliApiException(-1, "Download failed after $MAX_RETRIES retries")
    }

    private fun attemptSinglePass(
        url: String,
        destination: File,
        alreadyDownloaded: Long,
        onProgress: (Long) -> Unit,
    ): Long {
        destination.parentFile?.mkdirs()
        destination.createNewFile()
        val rangeHeader = if (alreadyDownloaded > 0) "bytes=$alreadyDownloaded-" else null
        val builder = Request.Builder().url(url)
        if (rangeHeader != null) builder.header("Range", rangeHeader)
        val request = builder.build()
        client.newCall(request).execute().use { response ->
            if (response.code == 403) throw BiliApiException(403, "CDN returned 403")
            if (!response.isSuccessful && response.code != 206) {
                throw BiliApiException(response.code, "Download failed: ${response.code}")
            }
            val body = response.body ?: throw BiliApiException(-1, "Empty download body")
            var downloaded = alreadyDownloaded
            RandomAccessFile(destination, "rw").use { output ->
                output.seek(alreadyDownloaded)
                val buffer = ByteArray(CHUNK_SIZE)
                while (true) {
                    if (Thread.currentThread().isInterrupted) throw CancellationException("Interrupted")
                    val read = body.byteStream().read(buffer)
                    if (read < 0) break
                    output.write(buffer, 0, read)
                    downloaded += read
                    onProgress(downloaded)
                }
            }
            return downloaded
        }
    }

    companion object {
        private const val TAG = "BiliChunkDownloader"
        private const val MAX_RETRIES = 3
        private const val BASE_BACKOFF_MS = 1000L
        private const val JITTER_MIN_MS = 100L
        private const val JITTER_MAX_MS = 300L
        private const val CHUNK_SIZE = 1 shl 20
    }
}

class LinkExpiredException(
    val expiredUrl: String,
    cause: Throwable,
) : Exception("CDN link expired: $expiredUrl", cause)