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
    var wantsNotifyV2: Boolean = false,
    var isTracked: Boolean = false,
    var photoPath: String? = null,
    var createdAt: Long = System.currentTimeMillis(),
)