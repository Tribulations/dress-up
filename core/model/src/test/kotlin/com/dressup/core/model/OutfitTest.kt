package com.dressup.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class OutfitTest {
    @Test
    fun `a rotation is kept as the same turn within -180 to 180`() {
        assertEquals(0f, normalizedRotation(360f), 0.001f)
        assertEquals(-90f, normalizedRotation(270f), 0.001f)
        assertEquals(90f, normalizedRotation(-270f), 0.001f)
        assertEquals(180f, normalizedRotation(540f), 0.001f)
        assertEquals(-15f, normalizedRotation(-15f), 0.001f)
        assertEquals(0f, normalizedRotation(Float.NaN), 0.001f)
    }

    @Test
    fun `board colour keys are unique`() {
        assertEquals(BoardColour.entries.size, BoardColour.entries.map { it.key }.toSet().size)
        assertEquals(BoardColour.SAGE, BoardColour.fromKeyOrNull("sage"))
    }
}
