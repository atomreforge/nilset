package net.atomreforge.nilset.bili.download

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import net.atomreforge.nilset.bili.api.BiliApiException
import net.atomreforge.nilset.bili.api.BiliApiService
import net.atomreforge.nilset.bili.api.BiliLogger
import net.atomreforge.nilset.bili.model.BiliAudioQuality
import net.atomreforge.nilset.bili.model.BiliMergeOutcome
import net.atomreforge.nilset.bili.model.BiliQuality
import net.atomreforge.nilset.bili.model.BiliStreamSelector
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

class BiliDownloadEngine(
    private val client: OkHttpClient,
    private val apiService: BiliApiService,
    private val snapshotStore: BiliSnapshotStore,
    private val tempDirectory: File,
    maxConcurrentTasks: Int = DEFAULT_CONCURRENT_TASKS,
    externalScope: CoroutineScope? = null,
) {
    private val scope = externalScope ?: CoroutineScope(SupervisorJob())
    private val chunkDownloader = BiliChunkDownloader(client)
    private val streamSelector = BiliStreamSelector()
    private val semaphore = Semaphore(maxConcurrentTasks)
    private val tasksMutex = Mutex()
    private val _tasks = MutableStateFlow<List<BiliDownloadTask>>(emptyList())
    val tasks: StateFlow<List<BiliDownloadTask>> = _tasks.asStateFlow()
    private val activeJobs = ConcurrentHashMap<String, Job>()

    suspend fun enqueue(request: BiliDownloadRequest): String {
        val taskId = "task_${System.currentTimeMillis()}_${Random.nextInt(1000, 9999)}"
        val task = BiliDownloadTask(taskId, request, "", BiliTaskState.QUEUED, BiliTaskProgress(), BiliQuality.UNKNOWN, BiliMergeOutcome.MERGED, null, null, null, true, System.currentTimeMillis())
        addTask(task)
        launchTask(taskId)
        return taskId
    }

    fun pause(taskId: String) { activeJobs[taskId]?.cancel(); scope.launch { updateTaskState(taskId, BiliTaskState.PAUSED) } }
    fun resume(taskId: String) { scope.launch { updateTaskState(taskId, BiliTaskState.QUEUED) }; launchTask(taskId) }
    fun cancel(taskId: String, deleteTemp: Boolean = false) { activeJobs[taskId]?.cancel(); activeJobs.remove(taskId); scope.launch { updateTaskState(taskId, BiliTaskState.CANCELLED); if (deleteTemp) cleanTemp(taskId) } }

    suspend fun restore() {
        for (s in snapshotStore.loadAll()) {
            val st = BiliTaskState.valueOf(s.state)
            if (st in ACTIVE_STATES) { val r = s.copy(state = BiliTaskState.PAUSED.name); snapshotStore.save(r); addTask(snap(r)) } else addTask(snap(s))
        }
    }

    private fun launchTask(taskId: String) {
        if (activeJobs.containsKey(taskId)) return
        activeJobs[taskId] = scope.launch { semaphore.acquire(); try { runTask(taskId) } finally { semaphore.release(); activeJobs.remove(taskId) } }
    }

    private suspend fun runTask(taskId: String) {
        try {
            val req = _tasks.value.firstOrNull { it.id == taskId }?.request ?: return
            updateTask(taskId) { it.copy(title = req.reference.bvid, state = BiliTaskState.DOWNLOADING) }
            val vp = File(tempDirectory, "${taskId}_v.m4s.part")
            val ap = File(tempDirectory, "${taskId}_a.m4s.part")
            var vd = vp.length()
            var ad = ap.length()
            var lastUpdate = 0L

            fun shouldUpdate(): Boolean {
                val now = System.currentTimeMillis()
                if (now - lastUpdate > 500) { lastUpdate = now; return true }
                return false
            }

            val vTotal = req.preResolvedVideoLength
            val aTotal = req.preResolvedAudioLength
            val videoUrl = req.preResolvedVideoUrl ?: run {
                updateTaskState(taskId, BiliTaskState.RESOLVING)
                val info = apiService.resolveVideo(req.reference.bvid)
                val cid = info.pages.firstOrNull()?.cid ?: return
                updateTask(taskId) { it.copy(title = info.title ?: "") }
                val playUrl = apiService.fetchPlayUrl(req.reference.bvid, cid)
                val dash = playUrl.dash ?: throw BiliApiException(-1, "No DASH")
                val qp = req.qualityPriority.map { BiliQuality.fromCode(it) }
                val ap2 = req.audioPriority.map { BiliAudioQuality.fromCode(it) }
                val sel = streamSelector.select(dash, qp, ap2, req.preferAvc)
                updateTaskState(taskId, BiliTaskState.DOWNLOADING)
                sel.videoStream.resolvedUrl
            }

            val vj = scope.launch {
                chunkDownloader.downloadToFile(videoUrl, vp, vp.length(), vTotal, onProgress = { bytes ->
                    vd = bytes
                    if (shouldUpdate()) scope.launch { progress(taskId, vd, vTotal, ad, aTotal) }
                })
            }
            val aUrl = req.preResolvedAudioUrl
            val aj = aUrl?.let { url ->
                scope.launch {
                    chunkDownloader.downloadToFile(url, ap, ap.length(), aTotal, onProgress = { })
                }
            }
            vj.join(); aj?.join()
            updateTaskState(taskId, BiliTaskState.COMPLETED)
        } catch (e: CancellationException) { updateTaskState(taskId, BiliTaskState.PAUSED) }
        catch (e: Exception) { BiliLogger.e(TAG, "Task $taskId failed", e); updateTask(taskId) { it.copy(state = BiliTaskState.FAILED, errorMessage = e.message, retryable = true) } }
    }

    private suspend fun progress(taskId: String, vd: Long, vt: Long, ad: Long, at: Long) {
        updateTask(taskId) { t -> t.copy(progress = BiliTaskProgress(vd, vt, ad, at, 0, -1)) }
    }
    private suspend fun addTask(t: BiliDownloadTask) { tasksMutex.withLock { _tasks.value = _tasks.value + t } }
    private suspend fun updateTask(id: String, f: (BiliDownloadTask) -> BiliDownloadTask) { tasksMutex.withLock { _tasks.value = _tasks.value.map { if (it.id == id) f(it) else it } } }
    private suspend fun updateTaskState(id: String, s: BiliTaskState) { updateTask(id) { it.copy(state = s) } }
    private fun snap(s: BiliTaskSnapshot) = BiliDownloadTask(s.id, s.request, s.title, BiliTaskState.valueOf(s.state), s.progress, BiliQuality.fromCode(s.selectedQualityCode), BiliMergeOutcome.valueOf(s.mergeOutcome), s.downgradeReason, null, s.errorMessage, s.state == BiliTaskState.FAILED.name, s.createdAt)
    private fun cleanTemp(id: String) { tempDirectory.listFiles()?.filter { it.name.startsWith(id) }?.forEach { it.delete() } }

    companion object {
        private const val TAG = "BiliDownloadEngine"
        const val DEFAULT_CONCURRENT_TASKS = 1
        private val ACTIVE_STATES = setOf(BiliTaskState.QUEUED, BiliTaskState.RESOLVING, BiliTaskState.DOWNLOADING, BiliTaskState.MERGING)
    }
}
