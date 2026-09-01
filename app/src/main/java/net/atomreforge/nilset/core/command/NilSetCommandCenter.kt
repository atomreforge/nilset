package net.atomreforge.nilset.core.command

import net.atomreforge.nilset.const.CommandExpressions

/**
 * 控制台指令分发中心（纯 Kotlin，不依赖 Android UI）。
 * 接收用户输入，通过 [CommandRegistry] 查找并执行对应指令。
 * 新增指令只需实现 [NilSetCommand] 并注册，不改此类。
 */
class NilSetCommandCenter(private val registry: CommandRegistry) {

    private fun dispatch(input: String, context: CommandContext): String {
        val command = input.trim()

        if (!command.startsWith(CommandExpressions.PREFIX)) {
            return "不是内部指令，请输入以 / 开头的指令（如 /help）"
        }

        val commandName = command.removePrefix(CommandExpressions.PREFIX)

        if (commandName == CommandExpressions.HELP) {
            return buildHelp()
        }

        val found = registry.find(commandName)
            ?: return "未知指令：$command（输入 /help 查看帮助）"

        if (found.isDebugOnly && !registry.isDebug) {
            return "未知指令：$command（输入 /help 查看帮助）"
        }

        return when (val result = found.execute(context)) {
            is CommandResult.Success -> result.message
            is CommandResult.Failure -> "错误：${result.message}"
        }
    }

    fun execute(input: String, context: CommandContext): String =
        dispatch(input, context)

    /** 返回当前构建可用的指令，供输入补全展示 */
    fun visibleCommands(): List<NilSetCommand> = registry.visibleCommands()

    private fun buildHelp(): String = buildString {
        appendLine("可用指令：")
        registry.visibleCommands().forEach { cmd ->
            appendLine("  /${cmd.name}   ${cmd.description}")
        }
    }.trimEnd()

}
