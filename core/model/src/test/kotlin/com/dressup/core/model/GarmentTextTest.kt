package com.dressup.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GarmentTextTest {
    @Test
    fun `blank text is stored as no text, other text trimmed and capped`() {
        assertNull(cleanText(null, 10))
        assertNull(cleanText("   ", 10))
        assertEquals("Coola", cleanText("  Coola  ", 10))
        assertEquals("abc", cleanText("abcdef", 3))
    }

    @Test
    fun `a cut-out is shown instead of the photo when there is one`() {
        val garment = Garment(1, null, null, listOf(1), emptyList(), "photo.jpg", null, null, emptyMap(), null, false, 0)
        assertEquals("photo.jpg", garment.pictureFileName)
        assertEquals("cutout.webp", garment.copy(cutoutFileName = "cutout.webp").pictureFileName)
    }

    @Test
    fun `new outfit garments are laid out on the board without covering each other`() {
        val items = defaultOutfitLayout(listOf(1, 2, 3, 4, 5, 6))
        assertEquals(6, items.map { it.x to it.y }.toSet().size)
        // Each garment's square (its width is scale × board width; the board is 4/3 as tall as wide) stays on the board.
        items.forEach { item ->
            val halfWidth = item.scale / 2
            val halfHeight = item.scale / 2 * 3 / 4
            assertTrue(item.x - halfWidth >= 0f && item.x + halfWidth <= 1f)
            assertTrue(item.y - halfHeight >= 0f && item.y + halfHeight <= 1f)
        }
    }

    @Test
    fun `aspect keys are unique and stable`() {
        assertEquals(GarmentAspect.entries.size, GarmentAspect.entries.map { it.key }.toSet().size)
        assertEquals(GarmentAspect.EASY_CARE, GarmentAspect.fromKeyOrNull("easy_care"))
    }
}
