package com.arbon.vata.floralens.data.photos

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PhotoDecodingTest {
    @Test
    fun `small photos keep their original resolution`() {
        assertEquals(1, PhotoDecoding.sampleSize(1024, 768))
        assertEquals(1, PhotoDecoding.sampleSize(1600, 1200))
    }

    @Test
    fun `large landscape and portrait photos have bounded decoded dimensions`() {
        for ((width, height) in listOf(8000 to 6000, 6000 to 8000, 4032 to 3024, 2000 to 1600)) {
            val sample = PhotoDecoding.sampleSize(width, height)
            assertTrue(maxOf(width, height) / sample <= PhotoDecoding.MAX_SIDE)
            assertEquals(0, sample and (sample - 1))
        }
    }
    @Test
    fun `HEIF detection handles major and compatible brands without treating JPEG as HEIF`() {
        assertTrue(PhotoDecoding.isHeif("0000ftypheic0000".toByteArray()))
        assertTrue(PhotoDecoding.isHeif("0000ftypxxxx0000mif1".toByteArray()))
        assertEquals(false, PhotoDecoding.isHeif("0000ftypavif0000".toByteArray()))
        assertEquals(false, PhotoDecoding.isHeif(byteArrayOf(0xff.toByte(), 0xd8.toByte())))
    }

}
