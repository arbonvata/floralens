package com.arbon.vata.floralens.domain.species

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SpeciesMapTest {

    @Test
    fun `maps full binomial names`() {
        assertEquals("Tomato", SpeciesMap.match("Solanum lycopersicum"))
        assertEquals("Bell Pepper", SpeciesMap.match("Capsicum annuum"))
        assertEquals("Avocado", SpeciesMap.match("Persea americana"))
    }

    @Test
    fun `genus fallback catches varieties the full table misses`() {
        assertEquals("Banana", SpeciesMap.match("Musa acuminata"))
        assertEquals("Banana", SpeciesMap.match("Musa × paradisiaca"))
        assertEquals("Strawberry", SpeciesMap.match("Fragaria virginiana"))
    }

    @Test
    fun `full name wins over genus so peach is not a cherry`() {
        assertEquals("Peach", SpeciesMap.match("Prunus persica"))
        assertEquals("Cherry", SpeciesMap.match("Prunus avium"))
    }

    @Test
    fun `plants outside the app scope return null`() {
        assertNull(SpeciesMap.match("Quercus robur"))
        assertNull(SpeciesMap.match(""))
    }

    @Test
    fun `supported flag splits the 14 crops from v2 species`() {
        assertTrue(SpeciesMap.isSupportedCrop("Tomato"))
        assertFalse(SpeciesMap.isSupportedCrop("Banana"))
        assertFalse(SpeciesMap.isSupportedCrop("Avocado"))
    }
}