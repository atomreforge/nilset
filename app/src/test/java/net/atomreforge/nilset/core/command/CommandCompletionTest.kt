package net.atomreforge.nilset.core.command

import net.atomreforge.nilset.core.command.commands.ConfigCommand
import net.atomreforge.nilset.core.command.commands.StatusCommand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandCompletionTest {
    private val commandCenter = NilSetCommandCenter(
        CommandRegistry(
            commands = listOf(ConfigCommand(), StatusCommand()),
            isDebug = true,
        ),
    )

    @Test
    fun `command name completion filters current prefix`() {
        val candidates = commandCenter.complete("/co", cursor = 3)

        assertEquals(listOf("/config"), candidates.map { it.displayText })
    }

    @Test
    fun `config completes actions after command and space`() {
        val candidates = commandCenter.complete("/config ", cursor = 8)

        assertEquals(
            listOf("list", "get", "set", "clear"),
            candidates.map { it.displayText },
        )
        assertEquals(8, candidates.first().replacementStart)
        assertEquals(8, candidates.first().replacementEnd)
        assertTrue(candidates.first().appendSpace)
    }

    @Test
    fun `config command name completes when whole command is typed`() {
        val candidates = commandCenter.complete("/config", cursor = 7)

        assertEquals(listOf("/config"), candidates.map { it.displayText })
    }

    @Test
    fun `config completes action from partial current segment`() {
        val candidates = commandCenter.complete("/config se", cursor = 10)

        assertEquals(listOf("set"), candidates.map { it.displayText })
    }

    @Test
    fun `config completes host option after set action and space`() {
        val candidates = commandCenter.complete("/config set ", cursor = 12)

        assertEquals(listOf("host_addr"), candidates.map { it.displayText })
    }

    @Test
    fun `value segment has no candidates after option and space`() {
        val candidates = commandCenter.complete(
            "/config set host_addr ",
            cursor = 22,
        )

        assertTrue(candidates.isEmpty())
    }

    @Test
    fun `command without arguments has no next segment candidates`() {
        val candidates = commandCenter.complete("/status ", cursor = 8)

        assertTrue(candidates.isEmpty())
    }

    @Test
    fun `slash shows all visible commands sorted by name`() {
        val candidates = commandCenter.complete("/", cursor = 1)

        assertEquals(
            listOf("/config", "/help", "/status"),
            candidates.map { it.displayText },
        )
        assertEquals(0, candidates.first().replacementStart)
        assertEquals(1, candidates.first().replacementEnd)
    }
}
