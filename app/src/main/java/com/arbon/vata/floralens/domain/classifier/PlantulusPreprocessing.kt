package com.arbon.vata.floralens.domain.classifier

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Plantulus requires shorter-side resize to 256, then a central 224 crop, RGB 0–255. */
internal object PlantulusPreprocessing {
    const val INPUT_SIZE = 224

    fun input(bitmap: Bitmap): ByteBuffer {
        val shorter = minOf(bitmap.width, bitmap.height)
        val width = (bitmap.width.toLong() * 256 / shorter).toInt()
        val height = (bitmap.height.toLong() * 256 / shorter).toInt()
        // Render only the central output instead of allocating a potentially huge
        // resized panorama. Rounded dimensions match shorter-side resizing.
        val output = Bitmap.createBitmap(INPUT_SIZE, INPUT_SIZE, Bitmap.Config.ARGB_8888)
        val left = (width - INPUT_SIZE) / 2
        val top = (height - INPUT_SIZE) / 2
        Canvas(output).drawBitmap(
            bitmap,
            null,
            RectF(-left.toFloat(), -top.toFloat(), (width - left).toFloat(), (height - top).toFloat()),
            Paint(Paint.FILTER_BITMAP_FLAG),
        )
        try {
            val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
            // A central 256 crop followed by central 224 crop has this same pixel offset.
            output.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
            return ByteBuffer.allocateDirect(4 * pixels.size * 3).order(ByteOrder.nativeOrder()).apply {
                pixels.forEach { pixel ->
                    putFloat(((pixel shr 16) and 0xFF).toFloat())
                    putFloat(((pixel shr 8) and 0xFF).toFloat())
                    putFloat((pixel and 0xFF).toFloat())
                }
                rewind()
            }
        } finally {
            output.recycle()
        }
    }
}
