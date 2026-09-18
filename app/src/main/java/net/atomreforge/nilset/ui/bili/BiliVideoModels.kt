package net.atomreforge.nilset.ui.bili

import net.atomreforge.nilset.bili.download.BiliTaskProgress
import net.atomreforge.nilset.bili.download.BiliTaskState
import net.atomreforge.nilset.bili.model.BiliMergeOutcome
import net.atomreforge.nilset.bili.model.BiliQuality
import net.atomreforge.nilset.bili.model.BiliVideoInfo

data class BiliVideoUiState(
    val inputText: String = "",
    val isResolving: Boolean = false,
    val videoInfo: BiliVideoInfo? = null,
    val availableQualities: List<BiliQuality> = emptyList(),
    val downloadableQualityCodes: Set<Int> = emptySet(),
    val selectedQuality: BiliQuality = BiliQuality.Q_1080P,
    val isEnqueuing: Boolean = false,
    val errorMessage: String? = null,
    val activeTaskId: String? = null,
    val taskState: BiliTaskState? = null,
    val taskProgress: BiliTaskProgress? = null,
    val mergeOutcome: BiliMergeOutcome? = null,
    val downgradeReason: String? = null,
    val completedOutputPath: String? = null,
)