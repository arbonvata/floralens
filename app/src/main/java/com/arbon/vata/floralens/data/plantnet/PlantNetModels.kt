package com.arbon.vata.floralens.data.plantnet

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire format of Pl@ntNet's `POST /v2/identify/all`, trimmed to what we use.
 * Every field defaults so unknown or absent JSON keys can never crash decoding —
 * the API adds fields regularly and we must tolerate them (`ignoreUnknownKeys`
 * plus defaults covers both shapes).
 */
@Serializable
data class PlantNetResponse(
    val results: List<PlantNetResult> = emptyList(),
)

/** One candidate species; `results` is not guaranteed sorted, so we maxByOrNull. */
@Serializable
data class PlantNetResult(
    /** Pl@ntNet's combined confidence for the species, 0..1. */
    val score: Float = 0f,
    val species: PlantNetSpecies = PlantNetSpecies(),
)

@Serializable
data class PlantNetSpecies(
    /** Binomial without author citation, e.g. "Musa acuminata" — what SpeciesMap matches on. */
    @SerialName("scientificNameWithoutAuthor") val scientificName: String = "",
    /** Locale-dependent names; first is usually the common English one. */
    val commonNames: List<String> = emptyList(),
)