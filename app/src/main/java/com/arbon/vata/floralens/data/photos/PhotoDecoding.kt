package com.arbon.vata.floralens.data.photos

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import java.io.InputStream

class UnsupportedHeicPhotoException : IllegalArgumentException()

/** Bounds allocations before decoding, including gallery and saved-photo cloud rechecks. */
internal object PhotoDecoding {
    const val MAX_SIDE = 1600

    fun sampleSize(width: Int, height: Int): Int {
        var sample = 1
        while (maxOf(width, height).toLong() > MAX_SIDE.toLong() * sample) sample *= 2
        return sample
    }

    // Inspect the container too: providers do not always supply a useful MIME type.
    fun isHeif(header: ByteArray): Boolean {
        if (header.size < 12 || String(header, 4, 4, Charsets.US_ASCII) != "ftyp") return false
        return (8..header.size - 4 step 4).any { offset ->
            offset != 12 && String(header, offset, 4, Charsets.US_ASCII) in
                setOf("heic", "heix", "hevc", "hevx", "heim", "heis", "hevm", "hevs", "mif1", "msf1")
        }
    }

    fun decode(openStream: () -> InputStream?): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val stream = openStream() ?: return null
        stream.buffered().use {
            it.mark(32)
            val header = ByteArray(32)
            var count = 0
            while (count < header.size) {
                val read = it.read(header, count, header.size - count)
                if (read <= 0) break
                count += read
            }
            if (Build.VERSION.SDK_INT < 28 && isHeif(header.copyOf(count))) {
                throw UnsupportedHeicPhotoException()
            }
            it.reset()
            BitmapFactory.decodeStream(it, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return openStream()?.use { BitmapFactory.decodeStream(it, null, options) }
    }
}
