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

    @Test
    fun `several colours are stored in their order and read back`() {
        val colours = listOf(GarmentColour.BLUE, GarmentColour.RED, GarmentColour.MULTI)
        assertEquals("blue,red,multi", GarmentColour.keysOf(colours))
        assertEquals(colours, GarmentColour.listFromKeys("blue,red,multi"))
    }

    @Test
    fun `a single stored colour, as before several were possible, still reads`() {
        assertEquals(listOf(GarmentColour.NAVY), GarmentColour.listFromKeys("navy"))
    }

    @Test
    fun `no colours, unknown ones and repeats`() {
        assertNull(GarmentColour.keysOf(emptyList()))
        assertEquals(emptyList<GarmentColour>(), GarmentColour.listFromKeys(null))
        assertEquals(emptyList<GarmentColour>(), GarmentColour.listFromKeys(""))
        assertEquals(listOf(GarmentColour.RED), GarmentColour.listFromKeys("teal,red,red"))
        assertEquals("red", GarmentColour.keysOf(listOf(GarmentColour.RED, GarmentColour.RED)))
    }
}
