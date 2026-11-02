package com.arbon.vata.floralens.debug

import android.content.Context
import com.arbon.vata.floralens.data.photos.PhotoDecoding
import com.arbon.vata.floralens.domain.classifier.ClassPrediction
import com.arbon.vata.floralens.domain.classifier.PlantClassifier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONArray

/** Runs actual offline inference without using fixture labels as predictions. */
object PlantDocEvaluation {
    data class Result(
        val file: String,
        val expectedLabel: String,
        val prediction: ClassPrediction? = null,
        val error: String? = null,
    ) {
        val matches: Boolean get() = prediction?.label == expectedLabel
    }

    suspend fun run(context: Context, classifier: PlantClassifier): List<Result> = withContext(Dispatchers.IO) {
        val samples = JSONArray(
            context.assets.open("plantdoc/samples.json").bufferedReader().use { it.readText() },
        )
        (0 until samples.length()).map { index ->
            ensureActive()
            val sample = samples.getJSONObject(index)
            val file = sample.getString("file")
            val expected = sample.getString("label")
            try {
                val bitmap = checkNotNull(PhotoDecoding.decode { context.assets.open("plantdoc/$file") }) {
                    "Could not decode photo"
                }
                try {
                    val prediction = classifier.classify(bitmap).maxByOrNull { it.confidence }
                    Result(file, expected, prediction, if (prediction == null) "No prediction returned" else null)
                } finally {
                    bitmap.recycle()
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Result(file, expected, error = error.message ?: "Inference failed")
            }
        }
    }
}
