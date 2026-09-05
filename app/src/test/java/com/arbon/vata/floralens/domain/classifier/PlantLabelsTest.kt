package com.arbon.vata.floralens.domain.classifier

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PlantLabelsTest {

    @Test
    fun `parses species, disease and health from a disease label`() {
        val parsed = PlantLabels.parse("Tomato___Early_blight")

        assertEquals("Tomato", parsed.species)
        assertEquals("Early blight", parsed.disease)
        assertEquals(false, parsed.isHealthy)
    }

    @Test
    fun `healthy labels carry no disease`() {
        val parsed = PlantLabels.parse("Apple___healthy")

        assertEquals("Apple", parsed.species)
        assertNull(parsed.disease)
        assertEquals(true, parsed.isHealthy)
    }

    @Test
    fun `bell pepper species name is normalized`() {
        val parsed = PlantLabels.parse("Pepper,_bell___Bacterial_spot")

        assertEquals("Bell Pepper", parsed.species)
        assertEquals("Bacterial spot", parsed.disease)
    }

    @Test
    fun `awkward dataset labels get friendly disease names`() {
        assertEquals(
            "Gray leaf spot",
            PlantLabels.parse("Corn___Cercospora_leaf_spot Gray_leaf_spot").disease,
        )
        assertEquals(
            "Spider mites",
            PlantLabels.parse("Tomato___Spider_mites Two-spotted_spider_mite").disease,
        )
        assertEquals(
            "Esca (Black Measles)",
            PlantLabels.parse("Grape___Esca_(Black_Measles)").disease,
        )
    }

    @Test
    fun `catalog matches the PlantVillage spec`() {
        assertEquals(38, PlantLabels.CLASSES.size)
        assertEquals(38, PlantLabels.CLASSES.toSet().size)

        val species = PlantLabels.CLASSES.map { PlantLabels.parse(it).species }.toSet()
        assertEquals(14, species.size)
        assertEquals(species, PlantLabels.SUPPORTED_SPECIES)
    }

    @Test
    fun `every label parses into a known species`() {
        PlantLabels.CLASSES.forEach { label ->
            val parsed = PlantLabels.parse(label)
            assertTrue(parsed.species in PlantLabels.SUPPORTED_SPECIES, "unmapped species in label: $label")
        }
    }
}