package com.arbon.vata.floralens.testdata

import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import com.arbon.vata.floralens.debug.PlantDocEvaluation
import com.arbon.vata.floralens.domain.classifier.ClassPrediction
import com.arbon.vata.floralens.domain.classifier.PlantClassifier
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlantDocEvaluationTest {
    @Test
    fun usesModelScoresInsteadOfFixtureLabelsAndReleasesPhotos() = runBlocking {
        val decoded = mutableListOf<Bitmap>()
        val classifier = object : PlantClassifier {
            override suspend fun classify(bitmap: Bitmap): List<ClassPrediction> {
                assertTrue(bitmap.width > 0 && bitmap.height > 0)
                decoded += bitmap
                return listOf(
                    ClassPrediction("not_the_winner", 0.1f),
                    ClassPrediction("model_prediction", 0.83f),
                )
            }
        }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val results = PlantDocEvaluation.run(context, classifier)
        assertEquals(5, results.size)
        results.forEach { result ->
            assertEquals("model_prediction", result.prediction?.label)
            assertEquals(0.83f, result.prediction!!.confidence, 0f)
            assertFalse(result.matches)
            assertEquals(null, result.error)
        }
        assertTrue(decoded.all { it.isRecycled })
    }

    @Test
    fun anInferenceFailureDoesNotPreventTheRemainingSamplesFromRunning() = runBlocking {
        var calls = 0
        val classifier = object : PlantClassifier {
            override suspend fun classify(bitmap: Bitmap): List<ClassPrediction> {
                calls++
                if (calls == 1) error("Test failure")
                return emptyList()
            }
        }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val results = PlantDocEvaluation.run(context, classifier)
        assertEquals(5, calls)
        assertEquals("Test failure", results.first().error)
        assertTrue(results.drop(1).all { it.error == "No prediction returned" })
    }
}
