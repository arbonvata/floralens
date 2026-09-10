package com.arbon.vata.floralens.data.photos

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.InputStream

/** Bounds allocations before decoding, including gallery and saved-photo cloud rechecks. */
internal object PhotoDecoding {
    const val MAX_SIDE = 1600

    fun sampleSize(width: Int, height: Int): Int {
        var sample = 1
        while (maxOf(width, height).toLong() > MAX_SIDE.toLong() * sample) sample *= 2
        return sample
    }

    fun decode(openStream: () -> InputStream?): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val stream = openStream() ?: return null
        stream.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return openStream()?.use { BitmapFactory.decodeStream(it, null, options) }
    }
}
