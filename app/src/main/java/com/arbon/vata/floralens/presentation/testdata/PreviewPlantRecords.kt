package com.arbon.vata.floralens.presentation.testdata

import com.arbon.vata.floralens.data.objectbox.PlantRecord

/**
 * Fixtures used as @Preview and default-parameter data in the screen composables.
 * Never reference these from production logic.
 */

/** Typical on-device diagnosis: confident, sick, no cloud round-trip needed. */
val tomatoEarlyBlight =
    PlantRecord(
        id = 1,
        detectedSpecies = "Tomato",
        commonName = "Tomato",
        diseaseName = "Early Blight",
        isHealthy = false,
        confidence = 0.82f,
        onDeviceLabel = "Tomato___Early_blight",
        isSupported = true,
    )

/** Same scan after a Pl@ntNet second opinion raised or confirmed the label. */
val tomatoEarlyBlightCloudVerified =
    tomatoEarlyBlight.copy(
        id = 2,
        isCloudVerified = true,
        hasHealthAssessment = true,
        cloudLabel = "Alternaria solani",
    )

val healthyApple =
    PlantRecord(
        id = 3,
        detectedSpecies = "Apple",
        commonName = "Apple",
        isHealthy = true,
        confidence = 0.96f,
        isSupported = true,
    )

/** Healthy verdict but below the 0.80 confidence gate — gets the yellow badge. */
val lowConfidenceCorn =
    PlantRecord(
        id = 4,
        detectedSpecies = "Corn",
        commonName = "Corn",
        isHealthy = true,
        confidence = 0.61f,
        isSupported = true,
    )

val bananaWishlist =
    PlantRecord(
        id = 5,
        detectedSpecies = "Banana",
        commonName = "Banana",
        isSupported = false,
        wantsNotifyV2 = true,
    )

val historyPreviewRecords: List<PlantRecord> =
    listOf(
        tomatoEarlyBlightCloudVerified,
        healthyApple,
        lowConfidenceCorn,
        bananaWishlist,
        healthyApple.copy(
            id = 6,
            detectedSpecies = "Grape",
            commonName = "Grape",
            confidence = 0.91f,
            createdAt = System.currentTimeMillis() - 3 * 24 * 60 * 60 * 1000L,
        ),
    )
/** One tomato followed over three scans: blight onset, treatment, recovery. */
val trackedPreviewRecords: List<PlantRecord> =
    listOf(
        tomatoEarlyBlight.copy(
            id = 11,
            isTracked = true,
            confidence = 0.95f,
            diseaseName = null,
            isHealthy = true,
            createdAt = System.currentTimeMillis() - 10 * 24 * 60 * 60 * 1000L,
        ),
        tomatoEarlyBlight.copy(
            id = 12,
            isTracked = true,
            confidence = 0.65f,
            createdAt = System.currentTimeMillis() - 3 * 24 * 60 * 60 * 1000L,
        ),
        tomatoEarlyBlightCloudVerified.copy(
            id = 13,
            isTracked = true,
            createdAt = System.currentTimeMillis(),
        ),
        healthyApple.copy(id = 14, isTracked = true),
    )
