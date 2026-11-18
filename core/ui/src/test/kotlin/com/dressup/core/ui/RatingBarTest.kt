package com.dressup.core.ui

import com.dressup.core.model.Rating
import org.junit.Assert.assertEquals
import org.junit.Test

class RatingBarTest {
    private val width = 100f // 5 stars of 20 px, half a star is 10 px

    @Test
    fun `a touch on the left half of the first star is half a star`() {
        assertEquals(1, halfStarsAt(4f, width))
    }

    @Test
    fun `a touch on the right half of the third star is three stars`() {
        assertEquals(6, halfStarsAt(55f, width))
    }

    @Test
    fun `touches outside the bar are clamped`() {
        assertEquals(1, halfStarsAt(-20f, width))
        assertEquals(10, halfStarsAt(140f, width))
        assertEquals(1, halfStarsAt(0f, width))
    }

    @Test
    fun `stars are formatted without a needless decimal`() {
        assertEquals("4", formatStars(Rating(8)))
        assertEquals("3.5", formatStars(Rating(7)))
    }
}
