package com.dressup.core.data.backup

import com.dressup.core.data.profile.uniqueName
import com.dressup.core.model.Profile
import com.dressup.core.model.ProfilesState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ProfileFileTest {
    private val file = ProfileFile(
        format = ProfileFile.FORMAT,
        version = ProfileFile.VERSION,
        app = "0.2.0",
        exportedAt = "2026-11-19T10:00:00Z",
        profile = ProfileInfo("Anna"),
        categories = listOf(CategoryFile(1, "T-shirt", builtIn = "tshirt"), CategoryFile(14, "Sportswear", hidden = true)),
        garments = listOf(
            GarmentFile(
                id = 3, name = "Coola favvis tröjan", categories = listOf(14, 1), colour = "beige",
                photo = "a.jpg", cutout = "b.webp", rating = 4.5, aspects = mapOf("fit" to 5.0),
                review = "Warm.\nItches a little: at the neck.", liked = true, added = "2026-01-02T03:04:05Z",
            ),
            GarmentFile(id = 4, name = "true", description = "42", review = "null", categories = listOf(1)),
        ),
        outfits = listOf(
            OutfitFile(id = 2, name = "Office", rating = 3.0, backgroundColour = "sage", items = listOf(OutfitItemFile(3, 0.5f, 0.25f, 0.45f, rotation = -15f))),
            OutfitFile(id = 5, backgroundImage = "images/bg.jpg", items = listOf(OutfitItemFile(4, 0.5f, 0.5f, 0.45f))),
        ),
        diary = listOf(DiaryFile(date = "2025-12-24", outfit = 2, garments = listOf(3, 4), occasion = "Christmas Eve", note = "Too warm # by the fire")),
    )

    @Test
    fun `a profile survives the trip through YAML unchanged`() {
        val text = ProfileYaml.encode(file)
        assertEquals(file, ProfileYaml.decode(text))
    }

    @Test
    fun `the YAML is readable and leaves out empty fields`() {
        val text = ProfileYaml.encode(file)
        println(text)
        assertTrue(text, text.contains("rating: 4.5"))
        assertTrue(text, text.contains("Coola favvis tröjan"))
        assertFalse("Empty fields are left out", text.contains(": null"))
    }

    @Test
    fun `a file from a newer version with extra keys still reads`() {
        val text = ProfileYaml.encode(file).replace("profile:\n", "favouriteColour: green\nprofile:\n")
        assertTrue(text.contains("favouriteColour"))
        assertEquals("Anna", ProfileYaml.decode(text).profile.name)
        assertEquals(ProfileYaml.Header("dress-up-profile", 1), ProfileYaml.header(text))
    }

    @Test
    fun `the header of some other YAML has no format`() {
        assertNull(ProfileYaml.header("name: something else\nitems: [1, 2]").format)
    }

    @Test
    fun `stars become half stars, out of range ones are dropped`() {
        assertEquals(9, ProfileImporter.halfStars(4.5))
        assertEquals(10, ProfileImporter.halfStars(5.0))
        assertEquals(1, ProfileImporter.halfStars(0.5))
        assertEquals(7, ProfileImporter.halfStars(3.4)) // rounded to the nearest half
        assertNull(ProfileImporter.halfStars(0.0))
        assertNull(ProfileImporter.halfStars(7.0))
        assertNull(ProfileImporter.halfStars(Double.NaN))
        assertNull(ProfileImporter.halfStars(null))
    }

    @Test
    fun `the suggested file name is safe and dated`() {
        assertEquals("dress-up-anna-2026-11-19.zip", exportFileName("Anna", LocalDate.of(2026, 11, 19)))
        assertEquals("dress-up-test-user-2026-11-19.zip", exportFileName("Test user", LocalDate.of(2026, 11, 19)))
        assertEquals("dress-up-åsa-b-2026-11-19.zip", exportFileName("Åsa / B.", LocalDate.of(2026, 11, 19)))
        assertEquals("dress-up-profile-2026-11-19.zip", exportFileName("///", LocalDate.of(2026, 11, 19)))
    }

    @Test
    fun `an imported profile gets a name of its own`() {
        val state = ProfilesState(listOf(Profile("main", "Anna", false), Profile("x", "Anna (2)", false), Profile("test", "", true)))
        assertEquals("Anna (3)", state.uniqueName("Anna", fallback = "Imported"))
        assertEquals("Bo", state.uniqueName("  Bo ", fallback = "Imported"))
        assertEquals("Imported", state.uniqueName("", fallback = "Imported"))
    }
}
