package com.arbon.vata.floralens.data.objectbox

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index

@Entity
data class PlantRecord(
    @Id var id: Long = 0,
    @Index var detectedSpecies: String = "",
    var commonName: String = "",
    var diseaseName: String? = null,
    var isHealthy: Boolean = true,
    var confidence: Float = 0f,
    var onDeviceLabel: String = "",
    var cloudLabel: String? = null,
    var isSupported: Boolean = true,
    var isCloudVerified: Boolean = false,
    // Separate from isCloudVerified: legacy rows were stamped isCloudVerified=true at
    // species-identification time, before any Plant.id health assessment existed.
    var hasHealthAssessment: Boolean = false,
    var wantsNotifyV2: Boolean = false,
    var isTracked: Boolean = false,
    var photoPath: String? = null,
    var createdAt: Long = System.currentTimeMillis(),
)
const val UNKNOWN_DIAGNOSIS_LABEL_PREFIX = "cloud_unknown:"

const val USER_CORRECTED_LABEL_PREFIX = "user_corrected:"
const val USER_CORRECTED_UNKNOWN_LABEL_PREFIX = "user_corrected_unknown:"

fun PlantRecord.isUserCorrected(): Boolean =
    onDeviceLabel.startsWith(USER_CORRECTED_LABEL_PREFIX) || onDeviceLabel.startsWith(USER_CORRECTED_UNKNOWN_LABEL_PREFIX)

fun PlantRecord.isDiagnosisUnknown(): Boolean =
    onDeviceLabel.startsWith(USER_CORRECTED_UNKNOWN_LABEL_PREFIX) || onDeviceLabel.startsWith(UNKNOWN_DIAGNOSIS_LABEL_PREFIX)

fun PlantRecord.classifierLabel(): String =
    onDeviceLabel.removePrefix(UNKNOWN_DIAGNOSIS_LABEL_PREFIX).removePrefix(USER_CORRECTED_LABEL_PREFIX).removePrefix(USER_CORRECTED_UNKNOWN_LABEL_PREFIX)
