package net.atomreforge.nilset.ui.settings

import net.atomreforge.nilset.data.session.UserInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class UserCardTest {

    @Test
    fun `uses nickname username and avatar from user info`() {
        val content = userCardContent(
            userInfo = UserInfo(
                uid = 1L,
                username = "alice",
                nickname = "Alice",
                avatar = "avatar-uri",
            ),
            fallbackUsername = "fallback",
            unknownLabel = "unknown",
        )

        assertEquals("Alice", content.nickname)
        assertEquals("alice", content.username)
        assertEquals("avatar-uri", content.avatar)
    }

    @Test
    fun `falls back to username when nickname is blank`() {
        val content = userCardContent(
            userInfo = UserInfo(uid = 1L, username = "alice", nickname = ""),
            fallbackUsername = "fallback",
            unknownLabel = "unknown",
        )

        assertEquals("alice", content.nickname)
    }

    @Test
    fun `falls back to session username then unknown label`() {
        val fromSession = userCardContent(null, "alice", "unknown")
        val unknown = userCardContent(null, null, "unknown")

        assertEquals("alice", fromSession.nickname)
        assertEquals("alice", fromSession.username)
        assertEquals("unknown", unknown.nickname)
        assertEquals("unknown", unknown.username)
        assertEquals("", unknown.avatar)
    }
}
