package com.arbon.vata.floralens.testdata

import android.graphics.Bitmap
import android.graphics.Color
import com.arbon.vata.floralens.domain.classifier.PlantulusPreprocessing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PlantulusPreprocessingTest {
    @Test
    fun keepsRgbInZeroTo255AndCropsTheCentre() {
        val pixels = IntArray(512 * 256) { index ->
            Color.rgb((index % 512) % 256, index / 512, 73)
        }
        val bitmap = Bitmap.createBitmap(pixels, 512, 256, Bitmap.Config.ARGB_8888)
        try {
            val input = PlantulusPreprocessing.input(bitmap)
            assertEquals(224 * 224 * 3 * 4, input.remaining())
            assertEquals(144f, input.float, 0f)
            assertEquals(16f, input.float, 0f)
            assertEquals(73f, input.float, 0f)
            assertFalse(bitmap.isRecycled)
        } finally {
            bitmap.recycle()
        }
    }

    @Test
    fun extremeAspectRatioDoesNotRequireAHugeIntermediateBitmap() {
        val bitmap = Bitmap.createBitmap(1600, 1, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.rgb(10, 20, 30))
        try {
            val input = PlantulusPreprocessing.input(bitmap)
            assertEquals(10f, input.float, 0f)
            assertEquals(20f, input.float, 0f)
            assertEquals(30f, input.float, 0f)
            assertFalse(bitmap.isRecycled)
        } finally {
            bitmap.recycle()
        }
    }
}
