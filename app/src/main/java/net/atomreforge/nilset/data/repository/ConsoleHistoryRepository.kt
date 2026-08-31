package net.atomreforge.nilset.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/** 保存当前进程内的控制台输出，避免导航返回后历史丢失 */
@Singleton
class ConsoleHistoryRepository @Inject constructor() {

    private val _entries = MutableStateFlow<List<String>>(emptyList())
    val entries: StateFlow<List<String>> = _entries.asStateFlow()

    fun append(entry: String) {
        _entries.update { it + entry }
    }

    fun clear() {
        _entries.value = emptyList()
    }
}
