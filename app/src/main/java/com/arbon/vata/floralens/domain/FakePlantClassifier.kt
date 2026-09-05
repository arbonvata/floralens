package com.arbon.vata.floralens.domain.classifier

import android.graphics.Bitmap
import kotlin.math.abs

/**
 * Deterministic stand-in for the LiteRT model so the whole scan pipeline runs
 * before plant_village.tflite ships. The same bitmap instance always yields the
 * same verdict; different captures produce varied species and confidences that
 * regularly cross the cloud gate.
 */
class FakePlantClassifier : PlantClassifier {
    override suspend fun classify(bitmap: Bitmap): List<ClassPrediction> {
        val seed = bitmap.generationId.toLong() * 31L + bitmap.width
        val primary = PlantLabels.CLASSES[Math.floorMod(seed, PlantLabels.CLASSES.size)]
        val secondary = PlantLabels.CLASSES[Math.floorMod(seed / 7, PlantLabels.CLASSES.size)]
        val primaryConfidence = 0.55f + Math.floorMod(seed / 100, 45) / 100f
        val secondaryConfidence = 0.04f + abs(Math.floorMod(seed / 13, 25)) / 100f
        return listOf(
            ClassPrediction(primary, primaryConfidence),
            ClassPrediction(secondary, secondaryConfidence),
        )
    }
}