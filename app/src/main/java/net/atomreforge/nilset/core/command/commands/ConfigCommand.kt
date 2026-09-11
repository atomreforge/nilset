package net.atomreforge.nilset.core.command.commands

import net.atomreforge.nilset.const.ConfigOptions
import net.atomreforge.nilset.core.command.CommandCompletion
import net.atomreforge.nilset.core.command.CommandCompletionContext
import net.atomreforge.nilset.core.command.CommandContext
import net.atomreforge.nilset.core.command.CommandResult
import net.atomreforge.nilset.core.command.NilSetCommand

class ConfigCommand : NilSetCommand {
    override val name = ConfigOptions.COMMAND
    override val description = "查看或设置配置"

    override fun completeArgument(
        context: CommandCompletionContext,
    ): List<CommandCompletion> {
        val currentPrefix = context.currentPrefix
        return when {
            context.hasCurrentArgument && context.completedArguments.isEmpty() ->
                actionCompletions(currentPrefix)

            context.hasCurrentArgument &&
                context.completedArguments == listOf(ConfigOptions.ACTION_SET) ->
                optionCompletions(currentPrefix)

            context.hasCurrentArgument &&
                (
                    context.completedArguments == listOf(ConfigOptions.ACTION_GET) ||
                        context.completedArguments == listOf(ConfigOptions.ACTION_CLEAR)
                    ) ->
                optionCompletions(currentPrefix)

            !context.hasCurrentArgument && context.completedArguments.isEmpty() ->
                actionCompletions(currentPrefix)

            !context.hasCurrentArgument &&
                (
                    context.completedArguments == listOf(ConfigOptions.ACTION_SET) ||
                        context.completedArguments == listOf(ConfigOptions.ACTION_GET) ||
                        context.completedArguments == listOf(ConfigOptions.ACTION_CLEAR)
                    ) ->
                optionCompletions(currentPrefix)

            else -> emptyList()
        }
    }

    override fun execute(context: CommandContext): CommandResult {
        val arguments = parseArguments(
            context.commandInput.removePrefix("/${ConfigOptions.COMMAND}"),
        )

        return when (arguments.firstOrNull()) {
            "list" -> requireExactly(arguments.size == 1) {
                CommandResult.Success(
                    context.configRepository.list().joinToString("\n") { entry ->
                        "${entry.option}|${entry.value}|default=${entry.defaultValue}"
                    },
                )
            }

            "get" -> requireExactly(arguments.size == 2) {
                context.configRepository.get(arguments[1]).fold(
                    onSuccess = { entry ->
                        CommandResult.Success(
                            "${entry.option}|${entry.value}|default=${entry.defaultValue}",
                        )
                    },
                    onFailure = { CommandResult.Failure(it.message ?: "读取失败") },
                )
            }

            "set" -> requireExactly(arguments.size == 3) {
                context.configRepository.set(arguments[1], arguments[2]).fold(
                    onSuccess = {
                        CommandResult.Success("已设置 ${arguments[1]}=${arguments[2]}")
                    },
                    onFailure = { CommandResult.Failure(it.message ?: "设置失败") },
                )
            }

            "clear" -> requireExactly(arguments.size == 2) {
                context.configRepository.clear(arguments[1]).fold(
                    onSuccess = {
                        CommandResult.Success("已恢复 ${arguments[1]} 默认值")
                    },
                    onFailure = { CommandResult.Failure(it.message ?: "恢复失败") },
                )
            }

            else -> CommandResult.Failure(USAGE)
        }
    }

    private fun parseArguments(input: String): List<String> {
        val argumentText = input.trim()
        if (argumentText.count { it == '"' } % 2 != 0) {
            return listOf(UNMATCHED_QUOTE)
        }

        return ARGUMENT_REGEX.findAll(argumentText)
            .map { match -> match.groupValues[1].ifEmpty { match.groupValues[2] } }
            .filter(String::isNotEmpty)
            .toList()
    }

    private inline fun requireExactly(
        condition: Boolean,
        action: () -> CommandResult,
    ): CommandResult = if (condition) action() else CommandResult.Failure(USAGE)

    private companion object {
        private val ACTION_COMPLETIONS = listOf(
            CommandCompletion(ConfigOptions.ACTION_LIST, "列出配置项"),
            CommandCompletion(ConfigOptions.ACTION_GET, "获取配置项"),
            CommandCompletion(ConfigOptions.ACTION_SET, "设置配置项"),
            CommandCompletion(ConfigOptions.ACTION_CLEAR, "恢复配置项默认值"),
        )
        private val OPTION_COMPLETIONS = listOf(
            CommandCompletion(ConfigOptions.HOST_ADDR, "服务器地址"),
        )
        val ARGUMENT_REGEX = Regex("\"([^\"]*)\"|(\\S+)")
        const val USAGE = "用法：/config list；/config get [option]；" +
            "/config set [option] [value]；/config clear [option]"
        const val UNMATCHED_QUOTE = "__unmatched_quote__"
    }

    private fun actionCompletions(prefix: String): List<CommandCompletion> =
        filterCompletions(ACTION_COMPLETIONS, prefix)

    private fun optionCompletions(prefix: String): List<CommandCompletion> =
        filterCompletions(OPTION_COMPLETIONS, prefix)

    private fun filterCompletions(
        completions: List<CommandCompletion>,
        prefix: String,
    ): List<CommandCompletion> = completions.filter {
        it.value.startsWith(prefix, ignoreCase = true)
    }
}
