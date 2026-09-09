package com.arbon.vata.floralens.data.plantid

import com.arbon.vata.floralens.domain.diagnosis.DiseaseAssessment
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire format of Plant.id's `health_assessment` endpoint. Tolerant on purpose:
 * v3 wraps the payload in a `result` object with suggestions nested under
 * `result.disease.suggestions`, while a flat v2-style shape also decodes — the
 * mapping falls back to the top-level fields when no wrapper is present. All
 * fields default so extra/missing keys never break us.
 */
@Serializable
data class PlantIdResponse(
    val result: PlantIdAssessment? = null,
    @SerialName("is_healthy") val flatIsHealthy: PlantIdHealth? = null,
    @SerialName("disease_suggestions") val flatDiseaseSuggestions: List<PlantIdDisease> = emptyList(),
)

@Serializable
data class PlantIdAssessment(
    @SerialName("is_healthy") val isHealthy: PlantIdHealth? = null,
    @SerialName("disease") val disease: PlantIdDiseaseSection? = null,
    @SerialName("disease_suggestions") val flatDiseaseSuggestions: List<PlantIdDisease> = emptyList(),
) {
    val diseaseSuggestions: List<PlantIdDisease>
        get() = disease?.suggestions ?: flatDiseaseSuggestions
}

/** v3 groups the disease suggestion list under a `disease` object in `result`. */
@Serializable
data class PlantIdDiseaseSection(
    val suggestions: List<PlantIdDisease> = emptyList(),
)

/** `binary` is the API's own healthy/sick call; `probability` its confidence (0..1). */
@Serializable
data class PlantIdHealth(
    val binary: Boolean? = null,
    val probability: Float = 0f,
)

@Serializable
data class PlantIdDisease(
    val name: String = "",
    val probability: Float = 0f,
)

/**
 * Collapses the payload into one verdict:
 * - healthy binary → healthy assessment at the API's confidence;
 * - otherwise the strongest disease suggestion (if it clears [minProbability])
 *   → diseased assessment;
 * - otherwise null — "sick but no confident disease" corrects nothing, so the
 *   on-device verdict stands unverified.
 */
internal fun PlantIdResponse.toAssessment(minProbability: Float = 0.1f): DiseaseAssessment? {
    val assessment =
        result ?: PlantIdAssessment(isHealthy = flatIsHealthy, flatDiseaseSuggestions = flatDiseaseSuggestions)
    val health = assessment.isHealthy ?: return null
    if (health.binary == true) {
        return DiseaseAssessment(isHealthy = true, diseaseName = null, confidence = health.probability)
    }
    val topDisease = assessment.diseaseSuggestions.maxByOrNull { it.probability } ?: return null
    if (topDisease.probability < minProbability || topDisease.name.isBlank()) return null
    return DiseaseAssessment(
        isHealthy = false,
        diseaseName = topDisease.name,
        confidence = topDisease.probability,
    )
}
