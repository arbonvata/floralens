package com.arbon.vata.floralens.domain.advice

import com.arbon.vata.floralens.domain.classifier.PlantLabels

/**
 * Static treatment guidance for the offline model's disease classes, keyed by
 * the display disease name produced by [PlantLabels.parse]. Diseases that
 * several crops share (early blight, bacterial spot, powdery mildew, black
 * rot) get one entry whose wording is correct on every crop the class list
 * pairs them with. Names that miss the map — e.g. free-text Plant.id diseases
 * — return null so the result screen can stay honest instead of guessing.
 */
object DiseaseAdvice {
    fun forDisease(disease: String?): List<String>? = disease?.let { ADVICE[it] }

    /** Looks up advice via the classifier label, covering user-corrected diagnoses. */
    fun forClassLabel(label: String): List<String>? = forDisease(PlantLabels.parse(label).disease)

    private val ADVICE: Map<String, List<String>> =
        mapOf(
            "Apple scab" to
                listOf(
                    "Remove fallen leaves in autumn; the fungus overwinters on them.",
                    "Prune for airflow and avoid wetting the leaves when watering.",
                    "If the tree was badly infected last season, spray sulfur or copper from bud break.",
                ),
            "Black rot" to
                listOf(
                    "Prune out dead or cankered wood and remove mummified fruit — both carry spores.",
                    "Clean up fallen leaves and fruit at the end of the season.",
                    "Protective fungicide in spring only if infections recur every year.",
                ),
            "Cedar apple rust" to
                listOf(
                    "Remove the orange galls from nearby junipers and cedars, the alternate host.",
                    "Pick off infected leaves and fruit early; infections stop in summer heat.",
                    "If rust returns yearly, a protective fungicide in spring reduces damage.",
                ),
            "Powdery mildew" to
                listOf(
                    "Improve airflow: thin crowded shoots and avoid high-nitrogen feeding.",
                    "Remove and dispose of heavily white-coated leaves or shoots.",
                    "Spray sulfur at the first signs, repeating in dry spells.",
                ),
            "Gray leaf spot" to
                listOf(
                    "Bury or remove corn residue after harvest; the fungus survives on it.",
                    "Rotate to a non-corn crop next season and pick a resistant hybrid.",
                    "Fungicide pays off only when lesions reach upper leaves before silking.",
                ),
            "Common rust" to
                listOf(
                    "Usually cosmetic: remove the worst leaves and let the plant outgrow it.",
                    "Avoid overhead watering; rust needs cool, moist leaf surfaces.",
                    "Choose a resistant hybrid next season; spraying is rarely worthwhile.",
                ),
            "Northern Leaf Blight" to
                listOf(
                    "Rotate crops and bury or remove last season's corn residue.",
                    "Plant a resistant hybrid next season.",
                    "If lesions climb toward the ear leaf around tasseling, apply a fungicide.",
                ),
            "Esca (Black Measles)" to
                listOf(
                    "No cure exists: prune out dead spurs and wood, cutting into healthy tissue.",
                    "Remove severely affected vines — they spread the fungus.",
                    "Prune in dry weather and disinfect tools between vines.",
                ),
            "Leaf blight (Isariopsis Leaf Spot)" to
                listOf(
                    "Remove and destroy affected leaves; do not leave them under the vine.",
                    "Pull leaves around fruit clusters to speed drying after rain.",
                    "Copper after bloom helps only in stretches of wet weather.",
                ),
            "Haunglongbing (Citrus greening)" to
                listOf(
                    "No cure exists. Confirm the diagnosis with a lab or extension service before acting.",
                    "Remove confirmed trees — keeping them infects nearby citrus via psyllids.",
                    "Control Asian citrus psyllids with horticultural oil or insecticidal soap.",
                    "Replant only certified disease-free trees.",
                ),
            "Bacterial spot" to
                listOf(
                    "Remove infected leaves and fruit; bacteria spread through splashing water.",
                    "Water at the base and handle plants only when dry.",
                    "Copper sprays slow new infections but rarely stop an outbreak.",
                    "Next season: rotate beds and choose resistant varieties.",
                ),
            "Early blight" to
                listOf(
                    "Pinch off infected lower leaves as soon as target-shaped spots appear.",
                    "Mulch around plants and water the soil, not the leaves.",
                    "Do not follow tomato with potato (or vice versa); they share the pathogen.",
                    "If lesions keep climbing the stem, a copper spray protects the rest.",
                ),
            "Late blight" to
                listOf(
                    "It spreads fast in cool, wet weather — check the plant every few days.",
                    "Remove infected leaves at once; destroy heavily hit plants, do not compost them.",
                    "Found early, a protective fungicide can save the crop.",
                    "Clear out volunteers and cull piles, which carry the pathogen over winter.",
                ),
            "Leaf scorch" to
                listOf(
                    "Remove infected leaves after harvest and renovate the bed.",
                    "Keep rows airy and use drip watering to keep leaves dry.",
                    "Spray only young, valuable beds; established plants usually grow out of it.",
                ),
            "Leaf Mold" to
                listOf(
                    "Ventilate: the mold takes hold in still, humid air, especially under cover.",
                    "Remove and dispose of the moldy lower leaves.",
                    "Choose a resistant variety next season.",
                ),
            "Septoria leaf spot" to
                listOf(
                    "Remove infected lower leaves; the disease climbs from the bottom up.",
                    "Mulch and water at the base so spores do not splash onto leaves.",
                    "Rotate beds and clear nightshade weeds, which also host it.",
                    "In a wet summer, copper sprays hold the spread down.",
                ),
            "Spider mites" to
                listOf(
                    "Hose down leaf undersides every few days; mites hate cool, damp conditions.",
                    "Remove the worst-infested leaves and keep the plant well watered.",
                    "Avoid broad-spectrum insecticides — they kill the mites' predators.",
                    "For heavy infestations, use insecticidal soap or horticultural oil.",
                ),
            "Target Spot" to
                listOf(
                    "Remove infected lower leaves before the spots ring together.",
                    "Mulch and water at the base to stop spore splash.",
                    "A fungicide helps only while weather stays wet; dry air halts it.",
                ),
            "Tomato Yellow Leaf Curl Virus" to
                listOf(
                    "No cure exists: remove young infected plants; older ones may still yield some fruit.",
                    "Fight the whitefly carrier: sticky traps plus soap or oil sprays.",
                    "Cover new plantings with insect netting and clear host weeds nearby.",
                    "Choose resistant varieties next season.",
                ),
            "Tomato mosaic virus" to
                listOf(
                    "No cure exists: remove infected plants entirely.",
                    "Wash hands and tools after touching infected plants — the virus spreads by contact.",
                    "Gardeners who smoke should wash hands first; tobacco carries a relative of this virus.",
                    "Grow resistant varieties and save seed only from healthy plants.",
                ),
        )
}
