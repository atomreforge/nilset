package net.atomreforge.nilset.ui.console

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import net.atomreforge.nilset.core.command.CommandContext
import net.atomreforge.nilset.core.command.NilSetCommandCenter
import net.atomreforge.nilset.data.repository.ConsoleHistoryRepository
import net.atomreforge.nilset.data.repository.SessionRepository
import javax.inject.Inject

/**
 * UI 层：控制台页 ViewModel。
 *
 * 职责：收集指令事件，交给核心层 [NilSetCommandCenter] 执行，把输出列表通过 [ConsoleUiState] 单向暴露给 UI。
 */
@HiltViewModel
class ConsoleViewModel @Inject constructor(
    private val consoleHistoryRepository: ConsoleHistoryRepository,
    private val sessionRepository: SessionRepository,
    private val commandCenter: NilSetCommandCenter,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConsoleUiState(consoleHistoryRepository.entries.value))
    val uiState: StateFlow<ConsoleUiState> = _uiState.asStateFlow()

    init {
        if (consoleHistoryRepository.entries.value.isEmpty()) {
            appendOutput("控制台已连接，输入 /help 查看可用指令。")
        }

        viewModelScope.launch {
            consoleHistoryRepository.entries.collect { entries ->
                _uiState.value = ConsoleUiState(entries)
            }
        }
    }

    /** 用户发送指令：交给核心层解析执行，结果追加进输出列表 */
    fun onCommandSent(command: String) {
        val trimmed = command.trim()
        if (trimmed.isEmpty()) return

        val context = CommandContext(consoleHistoryRepository, sessionRepository)
        val result = commandCenter.execute(trimmed, context)
        appendOutput("> $trimmed\n$result")
    }

    private fun appendOutput(text: String) {
        consoleHistoryRepository.append(text)
    }

}

/** 控制台 UI 状态：输出条目列表，由 UI 负责拼接渲染 */
data class ConsoleUiState(
    val entries: List<String> = emptyList(),
)
