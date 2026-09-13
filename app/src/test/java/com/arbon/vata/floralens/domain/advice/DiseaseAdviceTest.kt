package com.arbon.vata.floralens.domain.advice

import com.arbon.vata.floralens.domain.classifier.PlantLabels
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DiseaseAdviceTest {
    @Test
    fun `every offline disease class has non-empty advice`() {
        val diseased = PlantLabels.CLASSES.filterNot { PlantLabels.parse(it).isHealthy }
        assertTrue(diseased.isNotEmpty())
        diseased.forEach { label ->
            val advice = DiseaseAdvice.forClassLabel(label)
            assertTrue(!advice.isNullOrEmpty(), "missing advice for $label")
            advice!!.forEach { bullet -> assertTrue(bullet.isNotBlank(), "blank bullet for $label") }
        }
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
