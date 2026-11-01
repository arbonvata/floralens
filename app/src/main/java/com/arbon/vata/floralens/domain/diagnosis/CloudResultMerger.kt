package com.arbon.vata.floralens.domain.diagnosis

import com.arbon.vata.floralens.data.objectbox.PlantRecord
import com.arbon.vata.floralens.data.objectbox.UNKNOWN_DIAGNOSIS_LABEL_PREFIX
import com.arbon.vata.floralens.data.objectbox.USER_CORRECTED_LABEL_PREFIX
import com.arbon.vata.floralens.data.objectbox.USER_CORRECTED_UNKNOWN_LABEL_PREFIX
import com.arbon.vata.floralens.data.objectbox.classifierLabel
import com.arbon.vata.floralens.domain.species.SpeciesMatch

/** Merges cloud answers while invalidating diagnoses when the identified species changes. */
object CloudResultMerger {
    fun merge(
        record: PlantRecord,
        speciesMatch: SpeciesMatch?,
        diseaseAssessment: DiseaseAssessment?,
    ): PlantRecord {
        var updated = record
        if (speciesMatch != null) {
            // PlantNetClient maps scientific names to the same canonical crop names as PlantLabels.
            val speciesChanged = !speciesMatch.commonName.equals(updated.detectedSpecies, ignoreCase = true)
            if (speciesChanged) {
                updated = updated.copy(
                    diseaseName = null,
                    isHealthy = false,
                    isCloudVerified = false,
                    hasHealthAssessment = false,
                    onDeviceLabel = UNKNOWN_DIAGNOSIS_LABEL_PREFIX + updated.classifierLabel(),
                    confidence = 0f,
                )
            }
            updated =
                updated.copy(
                    detectedSpecies = speciesMatch.commonName,
                    commonName = speciesMatch.commonName,
                    isSupported = speciesMatch.isSupported,
                    cloudLabel = speciesMatch.scientificName,
                    confidence = if (speciesMatch.isSupported) updated.confidence else speciesMatch.confidence,
                )
        }
        if (diseaseAssessment != null && speciesMatch?.isSupported != false) {
            updated =
                updated.copy(
                    isHealthy = diseaseAssessment.isHealthy,
                    diseaseName = if (diseaseAssessment.isHealthy) null else diseaseAssessment.diseaseName,
                    cloudLabel =
                        if (updated.isSupported) diseaseAssessment.diseaseName ?: "Healthy"
                        else updated.cloudLabel,
                    isCloudVerified = true,
                    hasHealthAssessment = true,
                    confidence = if (updated.isSupported) diseaseAssessment.confidence else updated.confidence,
                    onDeviceLabel =
                        if (updated.isSupported) {
                            updated.onDeviceLabel
                                .removePrefix(UNKNOWN_DIAGNOSIS_LABEL_PREFIX)
                                .removePrefix(USER_CORRECTED_LABEL_PREFIX)
                                .removePrefix(USER_CORRECTED_UNKNOWN_LABEL_PREFIX)
                        } else {
                            updated.onDeviceLabel
                        },
                )
        }
        return updated
    }

}
