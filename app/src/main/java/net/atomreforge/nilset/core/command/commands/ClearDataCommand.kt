package net.atomreforge.nilset.core.command.commands

import net.atomreforge.nilset.core.command.CommandContext
import net.atomreforge.nilset.core.command.CommandResult
import net.atomreforge.nilset.core.command.NilSetCommand

/** 清空会话数据（含 DataStore 持久化），仅 debug 构建可用 */
class ClearDataCommand : NilSetCommand {
    override val name = "clear:data"
    override val description = "清除全部会话信息"
    override val isDebugOnly = true

    override fun execute(context: CommandContext): CommandResult {
        context.sessionRepository.clear()
        return CommandResult.Success("已清除全部信息")
    }
}
