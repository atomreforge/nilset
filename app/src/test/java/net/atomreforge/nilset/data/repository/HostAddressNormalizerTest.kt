package net.atomreforge.nilset.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HostAddressNormalizerTest {
    @Test
    fun `normalizes quoted ip address`() {
        val result = HostAddressNormalizer.normalize("\"192.168.101.1\"")

        assertEquals("192.168.101.1", result.getOrThrow())
    }

    @Test
    fun `accepts domain with port`() {
        val result = HostAddressNormalizer.normalize("syewiki.top:4703")

        assertEquals("syewiki.top:4703", result.getOrThrow())
    }

    @Test
    fun `rejects path`() {
        val result = HostAddressNormalizer.normalize("syewiki.top:4703/api")

        assertTrue(result.isFailure)
    }

    @Test
    fun `builds http base url`() {
        assertEquals(
            "http://syewiki.top:4703/",
            HostAddressNormalizer.toBaseUrl("syewiki.top:4703"),
        )
    }
}
