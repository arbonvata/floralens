package com.arbon.vata.floralens.domain.classifier

import android.graphics.Bitmap
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CascadeClassifierTest {

    private val classifier = mockk<PlantClassifier>()
    private val bitmap = mockk<Bitmap>()
    private val cascade = CascadeClassifier(classifier)

    private fun givenPredictions(vararg predictions: ClassPrediction) {
        coEvery { classifier.classify(any()) } returns predictions.toList()
    }

    // runBlocking<Unit> keeps the methods void: JUnit silently skips @Test methods
    // whose last expression (e.g. assertInstanceOf) leaks a non-Unit return type.
    @Test
    fun `confident verdict above both thresholds`() = runBlocking<Unit> {
        givenPredictions(
            ClassPrediction("Tomato___Early_blight", 0.92f),
            ClassPrediction("Tomato___healthy", 0.40f),
        )

        val verdict = cascade.diagnose(bitmap)

        assertInstanceOf(CascadeClassifier.Verdict.Confident::class.java, verdict)
        assertEquals("Tomato___Early_blight", verdict.prediction.label)
        assertEquals(0.92f, verdict.prediction.confidence)
    }

    @Test
    fun `second opinion needed when confidence below threshold`() = runBlocking<Unit> {
        givenPredictions(
            ClassPrediction("Corn___Northern_Leaf_Blight", 0.72f),
            ClassPrediction("Corn___healthy", 0.10f),
        )

        val verdict = cascade.diagnose(bitmap)

        assertInstanceOf(CascadeClassifier.Verdict.NeedsSecondOpinion::class.java, verdict)
    }

    @Test
    fun `second opinion needed when top two are too close`() = runBlocking<Unit> {
        givenPredictions(
            ClassPrediction("Apple___Black_rot", 0.95f),
            ClassPrediction("Apple___Apple_scab", 0.85f),
        )

        val verdict = cascade.diagnose(bitmap)

        assertInstanceOf(CascadeClassifier.Verdict.NeedsSecondOpinion::class.java, verdict)
        assertEquals("Apple___Black_rot", verdict.prediction.label)
    }

    @Test
    fun `thresholds are exclusive - exactly 0_80 confidence or 0_15 margin goes to cloud`() = runBlocking<Unit> {
        givenPredictions(
            ClassPrediction("Potato___Late_blight", 0.80f),
            ClassPrediction("Potato___healthy", 0.65f),
        )

        val verdict = cascade.diagnose(bitmap)

        assertInstanceOf(CascadeClassifier.Verdict.NeedsSecondOpinion::class.java, verdict)
    }

    @Test
    fun `predictions are ranked before applying the gate`() = runBlocking<Unit> {
        givenPredictions(
            ClassPrediction("Grape___healthy", 0.42f),
            ClassPrediction("Tomato___Late_blight", 0.97f),
            ClassPrediction("Grape___Black_rot", 0.30f),
        )

        val verdict = cascade.diagnose(bitmap)

        assertEquals("Tomato___Late_blight", verdict.prediction.label)
        assertEquals("Grape___healthy", verdict.runnerUp?.label)
        assertTrue(verdict.prediction.confidence > verdict.runnerUp!!.confidence)
    }

    @Test
    fun `single prediction with zero runner-up margin counts as high margin`() = runBlocking<Unit> {
        givenPredictions(ClassPrediction("Tomato___healthy", 0.99f))

        val verdict = cascade.diagnose(bitmap)

        assertInstanceOf(CascadeClassifier.Verdict.Confident::class.java, verdict)
        assertEquals(null, verdict.runnerUp)
    }
}