package com.arbon.vata.floralens.domain.advice

import com.arbon.vata.floralens.domain.classifier.PlantLabels
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DiseaseAdviceTest {
    @Test
    fun `existing crop disease classes retain reviewed advice`() {
        val newGroups = setOf("Aloe", "Cactus", "Money Plant", "Snake Plant", "Spider Plant")
        val diseased = PlantLabels.CLASSES.filter {
            val parsed = PlantLabels.parse(it)
            !parsed.isHealthy && parsed.species !in newGroups
        }
        assertTrue(diseased.isNotEmpty())
        diseased.forEach { label ->
            val advice = DiseaseAdvice.forClassLabel(label)
            assertTrue(!advice.isNullOrEmpty(), "missing advice for $label")
            advice!!.forEach { bullet -> assertTrue(bullet.isNotBlank(), "blank bullet for $label") }
        }
    }

    @Test
    fun `new indoor conditions have no invented treatment advice`() {
        val newGroups = setOf("Aloe", "Cactus", "Money Plant", "Snake Plant", "Spider Plant")
        val labels = PlantLabels.CLASSES.filter { PlantLabels.parse(it).species in newGroups }
        assertEquals(16, labels.size)
        labels.forEach { assertNull(DiseaseAdvice.forClassLabel(it), it) }
    }

    @Test
    fun `healthy and unknown names return no advice`() {
        assertNull(DiseaseAdvice.forClassLabel("Tomato___healthy"))
        assertNull(DiseaseAdvice.forDisease(null))
        assertNull(DiseaseAdvice.forDisease("Tomato late blight (Plant.id spelling)"))
    }

    @Test
    fun `diseases shared across crops resolve to one entry`() {
        val potato = DiseaseAdvice.forClassLabel("Potato___Early_blight")
        val tomato = DiseaseAdvice.forClassLabel("Tomato___Early_blight")
        assertEquals(potato, tomato)
        assertEquals(potato, DiseaseAdvice.forDisease("Early blight"))
    }
}
