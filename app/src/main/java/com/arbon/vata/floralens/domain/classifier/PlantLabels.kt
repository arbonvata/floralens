package com.arbon.vata.floralens.domain.classifier

/**
 * The 38 PlantVillage disease classes (14 species) the on-device model is trained on,
 * in training order — index i in this list is output index i of the model's softmax.
 */
object PlantLabels {
    val CLASSES: List<String> =
        listOf(
            "Apple___Apple_scab",
            "Apple___Black_rot",
            "Apple___Cedar_apple_rust",
            "Apple___healthy",
            "Blueberry___healthy",
            "Cherry___Powdery_mildew",
            "Cherry___healthy",
            "Corn___Cercospora_leaf_spot Gray_leaf_spot",
            "Corn___Common_rust_",
            "Corn___Northern_Leaf_Blight",
            "Corn___healthy",
            "Grape___Black_rot",
            "Grape___Esca_(Black_Measles)",
            "Grape___Leaf_blight_(Isariopsis_Leaf_Spot)",
            "Grape___healthy",
            "Orange___Haunglongbing_(Citrus_greening)",
            "Peach___Bacterial_spot",
            "Peach___healthy",
            "Pepper,_bell___Bacterial_spot",
            "Pepper,_bell___healthy",
            "Potato___Early_blight",
            "Potato___Late_blight",
            "Potato___healthy",
            "Raspberry___healthy",
            "Soybean___healthy",
            "Squash___Powdery_mildew",
            "Strawberry___Leaf_scorch",
            "Strawberry___healthy",
            "Tomato___Bacterial_spot",
            "Tomato___Early_blight",
            "Tomato___Late_blight",
            "Tomato___Leaf_Mold",
            "Tomato___Septoria_leaf_spot",
            "Tomato___Spider_mites Two-spotted_spider_mite",
            "Tomato___Target_Spot",
            "Tomato___Tomato_Yellow_Leaf_Curl_Virus",
            "Tomato___Tomato_mosaic_virus",
            "Tomato___healthy",
        )

    /** Species with full offline disease support in v1.0. */
    val SUPPORTED_SPECIES: Set<String> =
        setOf(
            "Apple", "Blueberry", "Cherry", "Corn", "Grape", "Orange", "Peach",
            "Bell Pepper", "Potato", "Raspberry", "Soybean", "Squash", "Strawberry", "Tomato",
        )

    data class ParsedLabel(
        val species: String,
        val disease: String?,
        val isHealthy: Boolean,
    )

    /** Splits a raw "Species___Disease" model label into display-friendly parts. */
    fun parse(label: String): ParsedLabel {
        val parts = label.split("___", limit = 2)
        val rawSpecies = parts.firstOrNull().orEmpty()
        val species =
            when (rawSpecies) {
                "Pepper,_bell" -> "Bell Pepper"
                else -> rawSpecies
            }
        val disease = parts.getOrNull(1)?.let { prettify(it) }
        val healthy = disease.equals("healthy", ignoreCase = true)
        return ParsedLabel(species = species, disease = if (healthy) null else disease, isHealthy = healthy)
    }

    private fun prettify(raw: String): String? =
        raw.replace('_', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
            .takeIf { it.isNotEmpty() }
            ?.let { SPECIAL_DISEASE_NAMES[it] ?: it }
}

private val SPECIAL_DISEASE_NAMES =
    mapOf(
        "Cercospora leaf spot Gray leaf spot" to "Gray leaf spot",
        "Spider mites Two-spotted spider mite" to "Spider mites",
    )