package com.arbon.vata.floralens.data.photos

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs

/**
 * Persists scan photos as JPEG files under the app-private "scan_photos" directory
 * and hands back a relative path for storage in ObjectBox. Keeping photos on disk
 * (instead of a byte[] in the entity) keeps the database small and lets Coil load
 * files directly by path.
 */
class ScanPhotoStore(
    private val context: Context,
) {
    fun save(bitmap: Bitmap): String {
        val file = newPhotoFile()
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        }
        return file.relativePath()
    }

    /** Copies gallery-imported bytes without re-encoding, preserving EXIF/rotation metadata. */
    fun save(uri: Uri): String? {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
        val file = newPhotoFile(extension = extensionOf(uri))
        bytes.inputStream().use { input -> file.outputStream().use { input.copyTo(it) } }
        return file.relativePath()
    }

    fun file(relativePath: String): File = File(context.filesDir, relativePath)

    /** Decodes a picked image for classification; null when the Uri is unreadable. */
    fun decode(uri: Uri): Bitmap? = context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)

    private fun newPhotoFile(extension: String = "jpg"): File {
        val dir = File(context.filesDir, PHOTO_DIR).apply { mkdirs() }
        return File(dir, "scan_${System.currentTimeMillis()}_${randomSuffix()}.$extension")
    }

    private fun extensionOf(uri: Uri): String {
        val mime = context.contentResolver.getType(uri)
        return when (mime) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/heic", "image/heif" -> "heic"
            else -> "jpg"
        }
    }

    private fun randomSuffix() = abs(System.nanoTime() % 100_000).toString().padStart(5, '0')

    private fun File.relativePath(): String = "$PHOTO_DIR/$name"

    private companion object {
        const val PHOTO_DIR = "scan_photos"
        const val JPEG_QUALITY = 85
    }
}