package com.arbon.vata.floralens.data.objectbox

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** user_corrected: prefix helpers — the markers correctDiagnosis writes and the UI reads back. */
class PlantRecordTest {

    @Test
    fun `records with either correction prefix count as user corrected`() {
        assertTrue(PlantRecord(onDeviceLabel = "user_corrected:Tomato___Late_blight").isUserCorrected())
        assertTrue(PlantRecord(onDeviceLabel = "user_corrected_unknown:Apple").isUserCorrected())
        assertFalse(PlantRecord(onDeviceLabel = "Tomato___Late_blight").isUserCorrected())
    }

    @Test
    fun `only species-only corrections count as unknown diagnosis`() {
        assertTrue(PlantRecord(onDeviceLabel = "user_corrected_unknown:Apple").isDiagnosisUnknown())
        assertFalse(PlantRecord(onDeviceLabel = "user_corrected:Tomato___Late_blight").isDiagnosisUnknown())
        assertFalse(PlantRecord(onDeviceLabel = "Tomato___Late_blight").isDiagnosisUnknown())
    }

    @Test
    fun `classifier label strips either prefix and leaves plain labels alone`() {
        assertEquals("Tomato___Late_blight", PlantRecord(onDeviceLabel = "user_corrected:Tomato___Late_blight").classifierLabel())
        assertEquals("Apple", PlantRecord(onDeviceLabel = "user_corrected_unknown:Apple").classifierLabel())
        assertEquals("Tomato___healthy", PlantRecord(onDeviceLabel = "Tomato___healthy").classifierLabel())
    }
}
