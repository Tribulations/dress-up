package com.dressup.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GarmentColourTest {
    @Test
    fun `colour keys are unique and round-trip`() {
        assertEquals(GarmentColour.entries.size, GarmentColour.entries.map { it.key }.toSet().size)
        GarmentColour.entries.forEach { assertEquals(it, GarmentColour.fromKeyOrNull(it.key)) }
    }

    @Test
    fun `unknown keys give no colour`() {
        assertNull(GarmentColour.fromKeyOrNull("teal"))
        assertNull(GarmentColour.fromKeyOrNull(null))
    }
}
