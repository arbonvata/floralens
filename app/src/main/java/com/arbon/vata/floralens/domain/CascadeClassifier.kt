package com.arbon.vata.floralens.domain.classifier

import android.graphics.Bitmap

/**
 * The cascade decision from the spec: trust the on-device model only when it is
 * confident (>0.80) AND the runner-up is far behind (top1 − top2 > 0.15);
 * otherwise flag the scan for a free cloud second opinion.
 */
class CascadeClassifier(
    private val classifier: PlantClassifier,
) {
    sealed interface Verdict {
        val prediction: ClassPrediction
        val runnerUp: ClassPrediction?

        /** ~90% of cases: show the on-device result instantly, $0 cost. */
        data class Confident(
            override val prediction: ClassPrediction,
            override val runnerUp: ClassPrediction?,
        ) : Verdict

        /** Low confidence or ambiguous: request a cloud second opinion downstream. */
        data class NeedsSecondOpinion(
            override val prediction: ClassPrediction,
            override val runnerUp: ClassPrediction?,
        ) : Verdict
    }

    suspend fun diagnose(bitmap: Bitmap): Verdict {
        val ranked = classifier.classify(bitmap).sortedByDescending { it.confidence }
        val top =
            ranked.firstOrNull()
                ?: error("Classifier returned no predictions")
        val runnerUp = ranked.getOrNull(1)
        val margin = top.confidence - (runnerUp?.confidence ?: 0f)
        return if (top.confidence > CONFIDENCE_THRESHOLD && margin > MARGIN_THRESHOLD) {
            Verdict.Confident(top, runnerUp)
        } else {
            Verdict.NeedsSecondOpinion(top, runnerUp)
        }
    }

    private companion object {
        const val CONFIDENCE_THRESHOLD = 0.80f
        const val MARGIN_THRESHOLD = 0.15f
    }
}