package com.arbon.vata.floralens.domain.diagnosis

import android.graphics.Bitmap

/** A cloud health assessment of one leaf photo, independent of the on-device model. */
data class DiseaseAssessment(
    val isHealthy: Boolean,
    /** Cloud disease name; null when the verdict is healthy. */
    val diseaseName: String?,
    val confidence: Float,
)

/**
 * Disease-level second opinion (Plant.id health assessment in v1.0). Runs only
 * when the on-device cascade gate is unsure, to confirm or correct the disease
 * label. Returns null when the service is unavailable (no key, offline, timeout)
 * so the on-device verdict stands.
 */
interface DiseaseVerifier {
    suspend fun assess(photo: Bitmap): DiseaseAssessment?
}