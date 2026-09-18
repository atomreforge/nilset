package net.atomreforge.nilset.ui.bili

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.atomreforge.nilset.bili.api.BiliApiService
import net.atomreforge.nilset.bili.api.BiliHttpClientFactory
import net.atomreforge.nilset.bili.api.BiliWbiSigner
import net.atomreforge.nilset.bili.download.BiliDownloadEngine
import net.atomreforge.nilset.bili.download.BiliDownloadRequest
import net.atomreforge.nilset.bili.download.BiliSnapshotStore
import net.atomreforge.nilset.bili.download.BiliTaskState
import net.atomreforge.nilset.bili.model.BiliQuality
import net.atomreforge.nilset.bili.model.BiliVideoReference
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BiliVideoEngineProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    @Volatile private var engine: BiliDownloadEngine? = null
    @Volatile private var apiService: BiliApiService? = null

    fun getEngine(): BiliDownloadEngine = engine ?: synchronized(this) { engine ?: create().also { engine = it.first; }.let { engine!! } }
    fun getApi(): BiliApiService = apiService ?: synchronized(this) { if (apiService == null) create(); apiService!! }

    private fun create(): Pair<BiliDownloadEngine, BiliApiService> {
        val factory = BiliHttpClientFactory.create(context)
        val client = factory.create()
        val signer = BiliWbiSigner(client)
        val api = BiliApiService(client, signer)
        val tempDir = File(context.cacheDir, "bili_nil_download")
        val snapshotFile = File(context.filesDir, "bili_download_snapshots.json")
        val store = BiliSnapshotStore(snapshotFile)
        val eng = BiliDownloadEngine(client, api, store, tempDir)
        apiService = api
        engine = eng
        return eng to api
    }
}

@HiltViewModel
class BiliVideoViewModel @Inject constructor(
    private val provider: BiliVideoEngineProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BiliVideoUiState())
    val uiState: StateFlow<BiliVideoUiState> = _uiState.asStateFlow()
    private var observeJob: Job? = null
    private var resolveJob: Job? = null

    init {
        observeTasks()
    }

    fun updateInput(value: String) { _uiState.update { it.copy(inputText = value) } }
    fun selectQuality(q: BiliQuality) { _uiState.update { it.copy(selectedQuality = q) } }

    fun resolve() {
        val s = _uiState.value
        if (s.isResolving || s.inputText.isBlank()) return
        resolveJob?.cancel()
        _uiState.update { it.copy(isResolving = true, errorMessage = null, videoInfo = null, availableQualities = emptyList()) }
        resolveJob = viewModelScope.launch {
            try {
                val bvid = extractBvid(s.inputText)
                val api = provider.getApi()
                val info = api.resolveVideo(bvid)
                val cid = info.pages.firstOrNull()?.cid ?: 0
                val play = api.fetchPlayUrl(bvid, cid)
                val qs = play.accept_quality.map { BiliQuality.fromCode(it) }.filter { it != BiliQuality.UNKNOWN }
                _uiState.update { it.copy(isResolving = false, videoInfo = info, availableQualities = qs) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { _uiState.update { it.copy(isResolving = false, errorMessage = e.message) } }
        }
    }

    fun enqueueDownload() {
        val s = _uiState.value
        if (s.videoInfo == null || s.isEnqueuing) return
        viewModelScope.launch {
            try {
                val bvid = extractBvid(s.inputText)
                val req = BiliDownloadRequest(
                    reference = BiliVideoReference(bvid = bvid),
                    qualityPriority = listOf(s.selectedQuality.code, 64, 32, 16),
                )
                val id = provider.getEngine().enqueue(req)
                _uiState.update { it.copy(isEnqueuing = false, activeTaskId = id) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { _uiState.update { it.copy(isEnqueuing = false, errorMessage = e.message) } }
        }
    }

    fun pauseTask() { _uiState.value.activeTaskId?.let { provider.getEngine().pause(it) } }
    fun resumeTask() { _uiState.value.activeTaskId?.let { provider.getEngine().resume(it) } }
    fun cancelTask() {
        _uiState.value.activeTaskId?.let { provider.getEngine().cancel(it, deleteTemp = true) }
        _uiState.update { BiliVideoUiState(inputText = it.inputText) }
    }

    private fun observeTasks() {
        observeJob = viewModelScope.launch {
            provider.getEngine().tasks.collect { tasks ->
                val id = _uiState.value.activeTaskId ?: return@collect
                val t = tasks.firstOrNull { it.id == id } ?: return@collect
                _uiState.update {
                    it.copy(
                        taskState = t.state, taskProgress = t.progress,
                        mergeOutcome = t.mergeOutcome, downgradeReason = t.downgradeReason,
                        completedOutputPath = if (t.state == BiliTaskState.COMPLETED) t.outputUri else null,
                    )
                }
            }
        }
    }

    private fun extractBvid(input: String): String {
        val t = input.trim()
        Regex("""BV([0-9A-Za-z]+)""", RegexOption.IGNORE_CASE).find(t)?.let { return "BV${it.groupValues[1]}" }
        return t
    }

    override fun onCleared() { observeJob?.cancel(); resolveJob?.cancel(); super.onCleared() }
}