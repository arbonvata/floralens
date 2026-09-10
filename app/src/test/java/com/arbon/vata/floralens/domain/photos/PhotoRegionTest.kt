package com.arbon.vata.floralens.domain.photos

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PhotoRegionTest {
    @Test
    fun `guide region stays within the decoded photo`() {
        assertEquals(PhotoRegion(0, 10, 100, 80), PhotoRegion(-5, 10, 110, 90).clampedTo(100, 80))
    }

    @Test
    fun `empty or offscreen regions cannot produce an invalid bitmap crop`() {
        assertEquals(PhotoRegion(99, 79, 100, 80), PhotoRegion(200, 200, 200, 200).clampedTo(100, 80))
        assertEquals(PhotoRegion(10, 10, 11, 11), PhotoRegion(10, 10, 0, 0).clampedTo(100, 80))
    }
}
