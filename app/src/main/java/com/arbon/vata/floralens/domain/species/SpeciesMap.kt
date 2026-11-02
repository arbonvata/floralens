package com.arbon.vata.floralens.domain.species

import com.arbon.vata.floralens.domain.classifier.PlantLabels

/**
 * Maps scientific names from species identification (Pl@ntNet) to the crop names
 * used in the app. Full binomial names win; the bare genus is the fallback, so
 * "Prunus persica" is a Peach, not a Cherry.
 */
object SpeciesMap {
    /** Exact binomials Pl@ntNet commonly returns for our crops. Checked first. */
    private val BY_FULL_NAME =
        mapOf(
            "aloe vera" to "Aloe",
            "aloe barbadensis" to "Aloe",
            "opuntia ficus-indica" to "Cactus",
            "epipremnum aureum" to "Money Plant",
            "dracaena trifasciata" to "Snake Plant",
            "sansevieria trifasciata" to "Snake Plant",
            "chlorophytum comosum" to "Spider Plant",
            "solanum lycopersicum" to "Tomato",
            "solanum tuberosum" to "Potato",
            "capsicum annuum" to "Bell Pepper",
            "malus domestica" to "Apple",
            "malus pumila" to "Apple",
            "prunus avium" to "Cherry",
            "prunus cerasus" to "Cherry",
            "prunus persica" to "Peach",
            "zea mays" to "Corn",
            "vitis vinifera" to "Grape",
            "citrus sinensis" to "Orange",
            "citrus × sinensis" to "Orange",
            "rubus idaeus" to "Raspberry",
            "glycine max" to "Soybean",
            "cucurbita pepo" to "Squash",
            "persea americana" to "Avocado",
            "mangifera indica" to "Mango",
        )

    /**
     * Genus-only fallback for crops where every species/cultivar means the same
     * thing to the user (any Musa is a Banana). Deliberately tiny: only genera we
     * treat as a single crop, so "Prunus" alone never silently beats the full-name
     * Peach/Cherry entries above.
     */
    private val BY_GENUS =
        mapOf(
            "musa" to "Banana",
            "vaccinium" to "Blueberry",
            "fragaria" to "Strawberry",
            "rubus" to "Raspberry",
            "cucurbita" to "Squash",
            "malus" to "Apple",
            "prunus" to "Cherry",
            "citrus" to "Orange",
        )

    /** Crop name for a scientific name, or null when the plant is outside our scope. */
    fun match(scientificName: String): String? {
        val normalized = scientificName.trim().lowercase()
        BY_FULL_NAME[normalized]?.let { return it }
        val genus = normalized.substringBefore(' ').trim()
        return BY_GENUS[genus]
    }

    fun isSupportedCrop(cropName: String): Boolean = cropName in PlantLabels.SUPPORTED_SPECIES
}