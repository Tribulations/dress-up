package com.dressup.core.data.testuser

import com.dressup.core.data.db.BUILT_IN_CATEGORIES
import org.junit.Assert.assertTrue
import org.junit.Test

class TestWardrobeTest {
    private val garments = TestWardrobe.garments

    @Test
    fun `every garment's category exists in a fresh database or among the test user's own`() {
        val known = BUILT_IN_CATEGORIES.map { it.first }.toSet() + TestWardrobe.OWN_CATEGORIES
        garments.forEach { assertTrue("Unknown category ${it.category}", it.category in known) }
    }

    @Test
    fun `every built-in category is used, so each filter chip has something to show`() {
        val used = garments.map { it.category }.toSet()
        BUILT_IN_CATEGORIES.forEach { (key, _) -> assertTrue("No garment in $key", key in used) }
    }

    @Test
    fun `the wardrobe has the edge cases worth testing`() {
        assertTrue(garments.any { it.picture == null })
        assertTrue(garments.any { it.colour == null })
        assertTrue(garments.any { it.halfStars == null })
        assertTrue(garments.any { it.halfStars != null && it.halfStars % 2 == 1 }) // a half star
        assertTrue(garments.any { it.liked } && garments.any { !it.liked })
        assertTrue(garments.all { it.halfStars == null || it.halfStars in 1..10 })
        assertTrue(garments.all { it.daysAgo >= 0 })
    }
}
