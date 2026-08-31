package net.atomreforge.nilset.core.command.commands

import net.atomreforge.nilset.core.command.CommandContext
import net.atomreforge.nilset.core.command.CommandResult
import net.atomreforge.nilset.core.command.NilSetCommand

/** 查看当前会话状态，所有构建可用 */
class StatusCommand : NilSetCommand {
    override val name = "status"
    override val description = "查看当前会话状态"

    override fun execute(context: CommandContext): CommandResult {
        val state = context.sessionRepository.sessionState.value
        val message = "已登录=${state.isLoggedIn}, 特殊模式=${state.isSpecialMode}, " +
            "用户名=${state.userInfo?.username ?: "无"}"
        return CommandResult.Success(message)
    }
}
