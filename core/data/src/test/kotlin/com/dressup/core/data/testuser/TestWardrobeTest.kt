package com.dressup.core.data.testuser

import com.dressup.core.data.backup.ProfileYaml
import com.dressup.core.data.backup.halfStars
import com.dressup.core.data.db.BUILT_IN_CATEGORIES
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.MonthDay

class TestWardrobeTest {
    private val garments = TestWardrobe.garments

    @Test
    fun `every garment's categories exist in a fresh database or among the test user's own`() {
        val known = BUILT_IN_CATEGORIES.map { it.first }.toSet() + TestWardrobe.OWN_CATEGORIES
        garments.forEach { garment ->
            assertTrue(garment.categories.isNotEmpty())
            garment.categories.forEach { assertTrue("Unknown category $it", it in known) }
            assertEquals("Duplicate category", garment.categories.size, garment.categories.toSet().size)
        }
    }

    @Test
    fun `every built-in category is used, so each filter chip has something to show`() {
        val used = garments.flatMap { it.categories }.toSet()
        BUILT_IN_CATEGORIES.forEach { (key, _) -> assertTrue("No garment in $key", key in used) }
    }

    @Test
    fun `the wardrobe has the edge cases worth testing`() {
        assertTrue(garments.any { it.picture == null })
        assertTrue(garments.any { it.colour == null })
        assertTrue(garments.any { it.colours.size > 1 })
        assertTrue(garments.any { it.halfStars == null })
        assertTrue(garments.any { it.halfStars != null && it.halfStars % 2 == 1 }) // a half star
        assertTrue(garments.any { it.liked } && garments.any { !it.liked })
        assertTrue(garments.any { it.name != null } && garments.any { it.name == null })
        assertTrue(garments.any { it.categories.size > 1 })
        assertTrue(garments.any { it.review != null } && garments.any { it.aspects.isNotEmpty() })
        assertTrue(garments.any { it.cutout } && garments.any { it.picture != null && !it.cutout })
        assertTrue(garments.all { it.halfStars == null || it.halfStars in 1..10 })
        assertTrue(garments.all { g -> g.aspects.values.all { it in 1..10 } })
        assertTrue(garments.all { it.daysAgo >= 0 })
        assertTrue(garments.none { it.cutout && it.picture == null })
    }

    @Test
    fun `outfits only use garments that have a key and a cut-out, each once`() {
        val keyed = garments.filter { it.key != null }.associateBy { it.key }
        assertEquals("Duplicate key", garments.count { it.key != null }, keyed.size)
        assertTrue(TestWardrobe.outfits.isNotEmpty())
        TestWardrobe.outfits.forEach { outfit ->
            val keys = outfit.garments.map { it.key }
            assertEquals(keys.size, keys.toSet().size)
            keys.forEach { key ->
                val garment = keyed[key]
                assertTrue("Unknown garment $key", garment != null)
                assertTrue("$key has no cut-out", garment!!.cutout)
            }
            outfit.garments.forEach { assertTrue(it.x in 0f..1f && it.y in 0f..1f && it.scale > 0f) }
        }
    }

    @Test
    fun `diary entries wear known outfits or keyed garments`() {
        val keys = garments.mapNotNull { it.key }.toSet()
        val outfitNames = TestWardrobe.outfits.map { it.name }.toSet()
        assertTrue(TestWardrobe.diary.any { it.outfit != null } && TestWardrobe.diary.any { it.outfit == null })
        assertTrue(TestWardrobe.diary.any { it.occasion != null } && TestWardrobe.diary.any { it.note != null })
        TestWardrobe.diary.forEach { entry ->
            assertTrue(entry.outfit != null || entry.garments.isNotEmpty())
            entry.outfit?.let { assertTrue("Unknown outfit $it", it in outfitNames) }
            entry.garments.forEach { assertTrue("Unknown garment $it", it in keys) }
        }
        TestWardrobe.outfits.forEach { outfit -> assertTrue(outfit.aspects.values.all { it in 1..10 }) }
    }

    @Test
    fun `a fixed day is the most recent one, never in the future`() {
        val christmasEve = TestDiaryEntry(monthDay = MonthDay.of(12, 24))
        assertEquals(LocalDate.of(2025, 12, 24), christmasEve.date(LocalDate.of(2026, 11, 19)))
        assertEquals(LocalDate.of(2026, 12, 24), christmasEve.date(LocalDate.of(2026, 12, 24)))
        assertEquals(LocalDate.of(2026, 11, 16), TestDiaryEntry(daysAgo = 3).date(LocalDate.of(2026, 11, 19)))
    }

    @Test
    fun `as an export file, everything links up, reads back the same and keeps its ratings`() {
        val pictures = garments.mapIndexed { i, g -> TestWardrobe.Pictures(g.picture?.let { "p$i.jpg" }, "c$i.png".takeIf { g.cutout }) }
        val file = TestWardrobe.toProfileFile(pictures, today = LocalDate.of(2026, 11, 24), now = 1_800_000_000_000)

        assertEquals(file, ProfileYaml.decode(ProfileYaml.encode(file)))
        assertEquals(garments.size, file.garments.size)
        assertEquals(garments.sortedByDescending { it.daysAgo }.map { it.name }, file.garments.map { it.name }) // oldest first
        val categoryIds = file.categories.map { it.id }.toSet()
        val garmentIds = file.garments.map { it.id }.toSet()
        file.garments.forEach { g -> assertTrue(g.categories.all { it in categoryIds }) }
        file.outfits.forEach { o -> assertTrue(o.items.all { it.garment in garmentIds }) }
        file.diary.forEach { e -> assertTrue(e.garments.isNotEmpty() && e.garments.all { it in garmentIds }) }
        assertEquals(garments.map { it.halfStars }.sortedBy { it }, file.garments.map { halfStars(it.rating) }.sortedBy { it })
        assertTrue(file.outfits.any { o -> o.items.any { it.locked } })
        assertTrue(file.garments.any { it.colours.size > 1 })
    }
}
