package com.arbon.vata.floralens.domain.species

import android.graphics.Bitmap

/** A species-level identification from the cloud (Pl@ntNet). */
data class SpeciesMatch(
    val commonName: String,
    val scientificName: String,
    val confidence: Float,
    val isSupported: Boolean,
)

/**
 * Free cloud species identification used as the cascade's second opinion.
 * Returns null when the service is unavailable (no API key, offline, timeout)
 * so the caller can fall back to the on-device result.
 */
interface SpeciesIdentifier {
    suspend fun identify(photo: Bitmap): SpeciesMatch?
}