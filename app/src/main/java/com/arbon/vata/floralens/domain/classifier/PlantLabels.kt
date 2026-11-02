package com.arbon.vata.floralens.domain.classifier

/**
 * The 54 Plantulus classes (19 plant groups), using canonical app labels,
 * in training order — index i in this list is output index i of the model's softmax.
 */
object PlantLabels {
    val CLASSES: List<String> =
        listOf(
            "Aloe___Anthracnose",
            "Aloe___healthy",
            "Aloe___Leaf_Spot",
            "Aloe___Rust",
            "Aloe___Sunburn",
            "Apple___Apple_scab",
            "Apple___Black_rot",
            "Apple___Cedar_apple_rust",
            "Apple___healthy",
            "Blueberry___healthy",
            "Cactus___Dactylopius_Opuntia_(Cochineal)",
            "Cactus___healthy",
            "Cherry___healthy",
            "Cherry___Powdery_mildew",
            "Corn___Cercospora_leaf_spot Gray_leaf_spot",
            "Corn___Common_rust_",
            "Corn___healthy",
            "Corn___Northern_Leaf_Blight",
            "Grape___Black_rot",
            "Grape___Esca_(Black_Measles)",
            "Grape___healthy",
            "Grape___Leaf_blight_(Isariopsis_Leaf_Spot)",
            "Money Plant___Bacterial_Wilt_Disease",
            "Money Plant___healthy",
            "Money Plant___Manganese_Toxicity",
            "Orange___Haunglongbing_(Citrus_greening)",
            "Peach___Bacterial_spot",
            "Peach___healthy",
            "Pepper,_bell___Bacterial_spot",
            "Pepper,_bell___healthy",
            "Potato___Early_blight",
            "Potato___healthy",
            "Potato___Late_blight",
            "Raspberry___healthy",
            "Snake Plant___Anthracnose",
            "Snake Plant___healthy",
            "Snake Plant___Leaf_Withering",
            "Soybean___healthy",
            "Spider Plant___Fungal_Leaf_Spot",
            "Spider Plant___healthy",
            "Spider Plant___Leaf_Tip_Necrosis",
            "Squash___Powdery_mildew",
            "Strawberry___healthy",
            "Strawberry___Leaf_scorch",
            "Tomato___Bacterial_spot",
            "Tomato___Early_blight",
            "Tomato___healthy",
            "Tomato___Late_blight",
            "Tomato___Leaf_Mold",
            "Tomato___Septoria_leaf_spot",
            "Tomato___Spider_mites Two-spotted_spider_mite",
            "Tomato___Target_Spot",
            "Tomato___Tomato_mosaic_virus",
            "Tomato___Tomato_Yellow_Leaf_Curl_Virus",
        )

    /** Plant groups represented by the model; disease coverage varies by group. */
    val SUPPORTED_SPECIES: Set<String> = CLASSES.map { it.substringBefore("___") }
        .map { if (it == "Pepper,_bell") "Bell Pepper" else it }.toSet()

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