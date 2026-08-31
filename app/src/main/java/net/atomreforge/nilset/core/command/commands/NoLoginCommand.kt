package net.atomreforge.nilset.core.command.commands

import net.atomreforge.nilset.core.command.CommandContext
import net.atomreforge.nilset.core.command.CommandResult
import net.atomreforge.nilset.core.command.NilSetCommand

/** 进入特殊模式（跳过登录），仅 debug 构建可用 */
class NoLoginCommand : NilSetCommand {
    override val name = "no:login"
    override val description = "跳过登录，进入特殊模式"
    override val isDebugOnly = true

    override fun execute(context: CommandContext): CommandResult {
        context.sessionRepository.enterSpecialMode()
        return CommandResult.Success("已进入特殊模式（跳过登录）")
    }
}
