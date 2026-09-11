package net.atomreforge.nilset.core.command

import net.atomreforge.nilset.data.repository.SessionRepository
import net.atomreforge.nilset.data.repository.ConsoleHistoryRepository
import net.atomreforge.nilset.data.repository.ConfigRepository

/** 指令执行上下文：向指令暴露其所需的依赖 */
data class CommandContext(
    val consoleHistoryRepository: ConsoleHistoryRepository,
    val sessionRepository: SessionRepository,
    val commandInput: String,
    val configRepository: ConfigRepository,
)
