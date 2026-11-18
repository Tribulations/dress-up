package com.dressup.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RatingTest {
    @Test
    fun `half stars convert to stars`() {
        assertEquals(3.5f, Rating(7).stars)
    }

    @Test
    fun `stored values outside 1 to 10 mean not rated`() {
        assertNull(Rating.fromHalfStarsOrNull(0))
        assertNull(Rating.fromHalfStarsOrNull(11))
        assertNull(Rating.fromHalfStarsOrNull(null))
        assertEquals(Rating(10), Rating.fromHalfStarsOrNull(10))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `an invalid rating cannot be created`() {
        Rating(0)
    }
}
