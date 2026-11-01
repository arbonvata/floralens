package com.arbon.vata.floralens.testdata

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.test.platform.app.InstrumentationRegistry
import com.arbon.vata.floralens.domain.classifier.PlantLabels
import com.arbon.vata.floralens.domain.classifier.TFLitePlantClassifier
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import java.security.MessageDigest

/** Exact RGB pixels avoid JPEG/resize differences between Pillow and Android. */
class ModelReferenceTest {
    @Test
    fun androidInferenceMatchesDesktopReference() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val reference = JSONObject(
            instrumentation.context.assets.open("model_audit/reference.json").bufferedReader().use { it.readText() },
        )
        val canonical = org.json.JSONArray(instrumentation.targetContext.assets.open("plantulus/canonical-labels.json").bufferedReader().use { it.readText() })
        assertEquals(PlantLabels.CLASSES, (0 until canonical.length()).map { canonical.getString(it) })
        val bytes = instrumentation.targetContext.assets.open("plant_disease_model.tflite").use { it.readBytes() }
        val hash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        assertEquals("Regenerate reference when changing the model", reference.getString("model_sha256"), hash)
        val size = reference.getInt("image_size")
        val pixels = IntArray(size * size) { index ->
            val x = index % size
            val y = index / size
            Color.rgb((3 * x + y) % 256, (5 * y + x) % 256, (7 * x + 11 * y) % 256)
        }
        val bitmap = Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
        val classifier = TFLitePlantClassifier(instrumentation.targetContext)
        suspend fun compare(bitmap: Bitmap, expected: org.json.JSONArray) {
            val actual = classifier.classify(bitmap).associateBy { it.label }
            assertEquals(PlantLabels.CLASSES.size, expected.length())
            PlantLabels.CLASSES.forEachIndexed { index, label ->
                val score = expected.getDouble(index).toFloat()
                val prediction = actual[label]
                // The app intentionally omits model scores below 1%.
                if (score > 0.0101f) {
                    checkNotNull(prediction) { "Missing $label" }
                    assertEquals(label, score, prediction.confidence, 0.0001f)
                } else if (prediction != null) {
                    assertEquals(label, score, prediction.confidence, 0.0001f)
                }
            }
        }
        try {
            compare(bitmap, reference.getJSONArray("scores"))
        } finally {
            bitmap.recycle()
        }
        val leaves = reference.getJSONArray("leaf_references")
        for (index in 0 until leaves.length()) {
            val leaf = leaves.getJSONObject(index)
            val photo = instrumentation.context.assets.open("model_audit/${leaf.getString("file")}").use {
                checkNotNull(BitmapFactory.decodeStream(it))
            }
            try {
                compare(photo, leaf.getJSONArray("scores"))
            } finally {
                photo.recycle()
            }
        }
    }
}
