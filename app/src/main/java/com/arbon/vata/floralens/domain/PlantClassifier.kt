package com.arbon.vata.floralens.domain.classifier

import android.graphics.Bitmap

/** One softmax output of the model: a raw training label and its probability. */
data class ClassPrediction(
    val label: String,
    val confidence: Float,
)

/**
 * Runs on-device image classification. The real implementation wraps the LiteRT
 * (TFLite) model; tests and pre-model development use [FakePlantClassifier].
 */
interface PlantClassifier {
    suspend fun classify(bitmap: Bitmap): List<ClassPrediction>
}