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

    @Test
    fun `a touch lands on the topmost garment under it, skipping locked ones`() {
        // A 300 × 400 board: the coat is a 150 px square around (150, 200), the scarf a 60 px one around (150, 180) on top.
        val coat = OutfitItem(1, x = 0.5f, y = 0.5f, scale = 0.5f)
        val scarf = OutfitItem(2, x = 0.5f, y = 0.45f, scale = 0.2f)
        val items = listOf(coat, scarf)
        assertEquals(2L, items.topmostAt(150f, 180f, 300f, 400f))
        assertEquals(1L, items.topmostAt(100f, 260f, 300f, 400f))
        assertEquals(null, items.topmostAt(10f, 10f, 300f, 400f))
        assertEquals(1L, listOf(coat, scarf.copy(locked = true)).topmostAt(150f, 180f, 300f, 400f))
        assertEquals(null, listOf(coat.copy(locked = true)).topmostAt(150f, 200f, 300f, 400f))
    }

    @Test
    fun `a turned garment is hit where it is drawn`() {
        // A 100 px square around (150, 200) turned 45°: its corners now point up, down and to the sides.
        val turned = listOf(OutfitItem(1, x = 0.5f, y = 0.5f, scale = 1f / 3, rotation = 45f))
        assertEquals(null, turned.topmostAt(195f, 155f, 300f, 400f)) // an upright square's corner, now outside
        assertEquals(1L, turned.topmostAt(150f, 265f, 300f, 400f)) // below the upright square, inside the turned one
    }
}
