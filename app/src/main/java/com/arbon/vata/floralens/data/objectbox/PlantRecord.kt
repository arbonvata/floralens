package com.arbon.vata.floralens.data.objectbox

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id

@Entity
data class PlantRecord(
    @Id var id: Long = 0,
    var detectedSpecies: String = "",
    var commonName: String = "",
    var diseaseName: String? = null,
    var isHealthy: Boolean = true,
    var confidence: Float = 0f,
    var onDeviceLabel: String = "",
    var cloudLabel: String? = null,
    var isSupported: Boolean = true,
    var isCloudVerified: Boolean = false,
)