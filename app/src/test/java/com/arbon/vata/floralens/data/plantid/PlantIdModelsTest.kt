package com.arbon.vata.floralens.data.plantid

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PlantIdModelsTest {

    private val json =
        Json {
            ignoreUnknownKeys = true
        }

    @Test
    fun `decodes v3 wrapped health assessment and ignores unknown fields`() {
        val payload = """
            {
              "result": {
                "classification": [{"name": "Solanum lycopersicum", "probability": 0.93}],
                "is_healthy": {"binary": false, "probability": 0.07},
                "disease_suggestions": [
                  {
                    "id": 1234,
                    "name": "Tomato late blight",
                    "probability": 0.61,
                    "similar_images": [],
                    "disease_details": {"common_names": ["late blight"], "cause": "Phytophthora infestans"}
                  },
                  {"name": "Tomato early blight", "probability": 0.22}
                ],
                "is_plant": {"probability": 0.99}
              },
              "input": {"latitude": null, "longitude": null}
            }
        """.trimIndent()

        val assessment = json.decodeFromString<PlantIdResponse>(payload).toAssessment()

        assertEquals(false, assessment?.isHealthy)
        assertEquals("Tomato late blight", assessment?.diseaseName)
        assertEquals(0.61f, assessment?.confidence)
    }

    @Test
    fun `decodes flat v2-style payload as fallback`() {
        val payload = """
            {
              "is_healthy": {"binary": true, "probability": 0.94},
              "disease_suggestions": []
            }
        """.trimIndent()

        val assessment = json.decodeFromString<PlantIdResponse>(payload).toAssessment()

        assertTrue(assessment!!.isHealthy)
        assertNull(assessment.diseaseName)
        assertEquals(0.94f, assessment.confidence)
    }

    @Test
    fun `healthy binary wins over weak disease suggestions`() {
        val payload = """
            {
              "result": {
                "is_healthy": {"binary": true, "probability": 0.88},
                "disease_suggestions": [{"name": "Tomato leaf mold", "probability": 0.05}]
              }
            }
        """.trimIndent()

        val assessment = json.decodeFromString<PlantIdResponse>(payload).toAssessment()

        assertTrue(assessment!!.isHealthy)
        assertNull(assessment.diseaseName)
    }

    @Test
    fun `sick with no confident suggestion corrects nothing`() {
        val payload = """
            {
              "result": {
                "is_healthy": {"binary": false, "probability": 0.4},
                "disease_suggestions": [{"name": "Something vague", "probability": 0.08}]
              }
            }
        """.trimIndent()

        val assessment = json.decodeFromString<PlantIdResponse>(payload).toAssessment()

        assertNull(assessment)
    }

    @Test
    fun `missing health block yields null`() {
        val payload = """{"result": {"disease_suggestions": []}}"""

        assertNull(json.decodeFromString<PlantIdResponse>(payload).toAssessment())
    }

    @Test
    fun `picks the strongest suggestion regardless of order`() {
        val payload = """
            {
              "result": {
                "is_healthy": {"binary": false, "probability": 0.1},
                "disease_suggestions": [
                  {"name": "Weak guess", "probability": 0.12},
                  {"name": "Strong guess", "probability": 0.77}
                ]
              }
            }
        """.trimIndent()

        val assessment = json.decodeFromString<PlantIdResponse>(payload).toAssessment()

        assertFalse(assessment!!.isHealthy)
        assertEquals("Strong guess", assessment.diseaseName)
        assertEquals(0.77f, assessment.confidence)
    }
}