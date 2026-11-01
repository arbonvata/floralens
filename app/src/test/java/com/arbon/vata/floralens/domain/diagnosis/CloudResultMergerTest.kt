package com.arbon.vata.floralens.domain.diagnosis

import com.arbon.vata.floralens.data.objectbox.PlantRecord
import com.arbon.vata.floralens.data.objectbox.isDiagnosisUnknown
import com.arbon.vata.floralens.domain.species.SpeciesMatch
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CloudResultMergerTest {
    private val record = PlantRecord(
        id = 7, detectedSpecies = "Tomato", commonName = "Tomato",
        diseaseName = "Late blight", confidence = 0.7f,
        onDeviceLabel = "Tomato___Late_blight", isTracked = true,
        photoPath = "scan_photos/leaf.jpg",
    )

    @Test
    fun `changed supported species invalidates old disease and confidence`() {
        val merged = CloudResultMerger.merge(record, SpeciesMatch("Corn", "Zea mays", 0.9f, true), null)
        assertEquals("Corn", merged.detectedSpecies)
        assertNull(merged.diseaseName)
        assertEquals(0f, merged.confidence)
        assertFalse(merged.isCloudVerified)
        assertTrue(merged.isDiagnosisUnknown())
        assertTrue(merged.isTracked)
        assertEquals(record.photoPath, merged.photoPath)
    }

    @Test
    fun `same species preserves offline diagnosis and its confidence`() {
        val merged = CloudResultMerger.merge(record, SpeciesMatch("tomato", "Solanum lycopersicum", 0.99f, true), null)
        assertEquals(record.diseaseName, merged.diseaseName)
        assertEquals(record.confidence, merged.confidence)
        assertFalse(merged.isDiagnosisUnknown())
    }

    @Test
    fun `supported health assessment supplies disease confidence after species change`() {
        val merged = CloudResultMerger.merge(record, SpeciesMatch("Corn", "Zea mays", 0.99f, true),
            DiseaseAssessment(false, "Rust", 0.85f))
        assertEquals("Rust", merged.diseaseName)
        assertEquals(0.85f, merged.confidence)
        assertTrue(merged.isCloudVerified)
        assertTrue(merged.hasHealthAssessment)
        assertFalse(merged.isDiagnosisUnknown())
    }

    @Test
    fun `unsupported identification ignores simultaneous disease answer`() {
        val merged = CloudResultMerger.merge(record, SpeciesMatch("Banana", "Musa", 0.91f, false),
            DiseaseAssessment(true, null, 0.98f))
        assertFalse(merged.isSupported)
        assertFalse(merged.hasHealthAssessment)
        assertEquals(0.91f, merged.confidence)
        assertEquals("Musa", merged.cloudLabel)
        assertNull(merged.diseaseName)
    }

    @Test
    fun `explicit unsupported health check preserves species identity and confidence`() {
        val unsupported = record.copy(isSupported = false, detectedSpecies = "Banana", cloudLabel = "Musa")
        val merged = CloudResultMerger.merge(unsupported, null, DiseaseAssessment(true, null, 0.98f))
        assertTrue(merged.hasHealthAssessment)
        assertTrue(merged.isHealthy)
        assertNull(merged.diseaseName)
        assertEquals("Musa", merged.cloudLabel)
        assertEquals(unsupported.confidence, merged.confidence)
    }

    @Test
    fun `unavailable cloud answers preserve the record`() {
        assertEquals(record, CloudResultMerger.merge(record, null, null))
    }
}
