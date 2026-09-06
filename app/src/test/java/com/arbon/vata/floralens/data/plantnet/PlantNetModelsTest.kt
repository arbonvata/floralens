package com.arbon.vata.floralens.data.plantnet

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PlantNetModelsTest {

    private val json =
        Json {
            ignoreUnknownKeys = true
        }

    @Test
    fun `parses a realistic identify-all response and ignores unknown fields`() {
        val payload = """
            {
              "query": {"project": "all", "images": ["leaf.jpg"], "organs": ["leaf"]},
              "language": "en",
              "preferedReferential": "the-plant-list",
              "results": [
                {
                  "score": 0.87231,
                  "species": {
                    "scientificNameWithoutAuthor": "Musa acuminata",
                    "scientificName": "Musa acuminata Colla",
                    "genus": {"scientificNameWithoutAuthor": "Musa"},
                    "family": {"scientificNameWithoutAuthor": "Musaceae"},
                    "commonNames": ["Banana", "Dwarf Cavendish banana"],
                    "gbif": {"id": "2767712"}
                  },
                  "images": []
                },
                {
                  "score": 0.04,
                  "species": {
                    "scientificNameWithoutAuthor": "Strelitzia reginae",
                    "commonNames": ["Bird of paradise"]
                  }
                }
              ]
            }
        """.trimIndent()

        val response = json.decodeFromString<PlantNetResponse>(payload)

        assertEquals(2, response.results.size)
        assertEquals(0.87231f, response.results[0].score)
        assertEquals("Musa acuminata", response.results[0].species.scientificName)
        assertEquals(listOf("Banana", "Dwarf Cavendish banana"), response.results[0].species.commonNames)
    }

    @Test
    fun `missing optional arrays decode to defaults`() {
        val response = json.decodeFromString<PlantNetResponse>("""{"results": []}""")

        assertEquals(0, response.results.size)
    }
}