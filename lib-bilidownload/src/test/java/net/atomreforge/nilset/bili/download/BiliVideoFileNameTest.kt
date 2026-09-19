package net.atomreforge.nilset.bili.download

import org.junit.Assert.assertEquals
import org.junit.Test

class BiliVideoFileNameTest {

    @Test
    fun `wraps title with halfwidth quotes and separates fields with pipes`() {
        val fileName = BiliVideoFileName.create(
            title = "依是猫？！",
            bvid = "BV1y2tr6FEKX",
            qualityLabel = "4K",
        )

        assertEquals("\"依是猫？！\"|BV1y2tr6FEKX|4K.mp4", fileName)
    }

    @Test
    fun `replaces path separators and embedded double quotes`() {
        val fileName = BiliVideoFileName.create(
            title = "bad/title\\name\"1",
            bvid = "BV1",
            qualityLabel = "1080P",
        )

        assertEquals("\"bad_title_name”1\"|BV1|1080P.mp4", fileName)
    }

    @Test
    fun `falls back to bvid when title is blank`() {
        val fileName = BiliVideoFileName.create(
            title = "   ",
            bvid = "BV1y2tr6FEKX",
            qualityLabel = "4K",
        )

        assertEquals("\"BV1y2tr6FEKX\"|BV1y2tr6FEKX|4K.mp4", fileName)
    }
}
