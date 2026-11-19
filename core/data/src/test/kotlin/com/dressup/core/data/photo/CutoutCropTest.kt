package com.dressup.core.data.photo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CutoutCropTest {
    private val opaque = 0xFF336699.toInt()
    private val faint = 0x10FFFFFF

    private fun image(width: Int, height: Int, vararg opaquePixels: Pair<Int, Int>): IntArray {
        val pixels = IntArray(width * height) { faint }
        opaquePixels.forEach { (x, y) -> pixels[y * width + x] = opaque }
        return pixels
    }

    @Test
    fun `a fully transparent cut-out has nothing to keep`() {
        assertNull(opaqueBounds(image(10, 10), 10, 10))
    }

    @Test
    fun `the box covers every visible pixel, faint ones ignored`() {
        val bounds = opaqueBounds(image(10, 20, 2 to 3, 7 to 15), 10, 20, marginFraction = 0f)
        assertEquals(PixelBounds(left = 2, top = 3, right = 8, bottom = 16), bounds)
    }

    @Test
    fun `the margin never goes outside the picture`() {
        val bounds = opaqueBounds(image(10, 10, 0 to 0, 9 to 9), 10, 10, marginFraction = 0.5f)
        assertEquals(PixelBounds(0, 0, 10, 10), bounds)
    }
}
