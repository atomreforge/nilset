package net.atomreforge.nilset.core.command.commands

import kotlinx.coroutines.flow.MutableStateFlow
import net.atomreforge.nilset.core.command.CommandRegistry
import net.atomreforge.nilset.core.command.NilSetCommandCenter
import net.atomreforge.nilset.const.ConfigOptions
import net.atomreforge.nilset.core.command.CommandContext
import net.atomreforge.nilset.core.command.CommandResult
import net.atomreforge.nilset.data.remote.interceptor.FakeSessionRepository
import net.atomreforge.nilset.data.repository.ConfigEntry
import net.atomreforge.nilset.data.repository.ConfigRepository
import net.atomreforge.nilset.data.repository.ConsoleHistoryRepository
import net.atomreforge.nilset.data.session.SessionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigCommandTest {
    private val configRepository = FakeConfigRepository()

    @Test
    fun `set accepts quoted host address`() {
        val result = execute("/config set host_addr \"192.168.101.1\"")

        assertEquals("192.168.101.1", configRepository.setValue)
        assertEquals("已设置 host_addr=192.168.101.1", (result as CommandResult.Success).message)
    }

    @Test
    fun `list uses pipe separated format`() {
        val result = execute("/config list")

        assertEquals(
            "${ConfigOptions.HOST_ADDR}|${ConfigOptions.DEFAULT_HOST_ADDR}|" +
                "default=${ConfigOptions.DEFAULT_HOST_ADDR}",
            (result as CommandResult.Success).message,
        )
    }

    @Test
    fun `get uses pipe separated format with default`() {
        val result = execute("/config get host_addr")

        assertEquals(
            "${ConfigOptions.HOST_ADDR}|${ConfigOptions.DEFAULT_HOST_ADDR}|" +
                "default=${ConfigOptions.DEFAULT_HOST_ADDR}",
            (result as CommandResult.Success).message,
        )
    }

    @Test
    fun `invalid set usage fails`() {
        val result = execute("/config set host_addr")

        assertTrue(result is CommandResult.Failure)
    }

    @Test
    fun `command center routes config command with arguments`() {
        val commandCenter = NilSetCommandCenter(
            CommandRegistry(commands = listOf(ConfigCommand()), isDebug = true),
        )

        val result = commandCenter.execute(
            "/config set host_addr \"192.168.101.1\"",
            CommandContext(
                consoleHistoryRepository = ConsoleHistoryRepository(),
                sessionRepository = FakeSessionRepository(SessionState()),
                commandInput = "/config set host_addr \"192.168.101.1\"",
                configRepository = configRepository,
            ),
        )

        assertEquals("192.168.101.1", configRepository.setValue)
        assertEquals("已设置 host_addr=192.168.101.1", result)
    }

    private fun execute(input: String): CommandResult = ConfigCommand().execute(
        CommandContext(
            consoleHistoryRepository = ConsoleHistoryRepository(),
            sessionRepository = FakeSessionRepository(SessionState()),
            commandInput = input,
            configRepository = configRepository,
        ),
    )

    private class FakeConfigRepository : ConfigRepository {
        override val baseUrl = MutableStateFlow("http://${ConfigOptions.DEFAULT_HOST_ADDR}/")

        var setValue: String? = null
            private set

        override fun get(option: String): Result<ConfigEntry> = Result.success(
            ConfigEntry(option, ConfigOptions.DEFAULT_HOST_ADDR, ConfigOptions.DEFAULT_HOST_ADDR),
        )

        override fun set(option: String, value: String): Result<Unit> {
            if (option != ConfigOptions.HOST_ADDR) {
                return Result.failure(IllegalArgumentException("未知配置项：$option"))
            }
            setValue = value
            return Result.success(Unit)
        }

        override fun clear(option: String): Result<Unit> = Result.success(Unit)

        override fun list(): List<ConfigEntry> = listOf(
            ConfigEntry(
                ConfigOptions.HOST_ADDR,
                ConfigOptions.DEFAULT_HOST_ADDR,
                ConfigOptions.DEFAULT_HOST_ADDR,
            ),
        )
    }
}
