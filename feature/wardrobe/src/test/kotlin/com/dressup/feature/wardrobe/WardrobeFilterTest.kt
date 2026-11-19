package com.dressup.feature.wardrobe

import com.dressup.core.model.Category
import com.dressup.core.model.Garment
import com.dressup.core.model.GarmentColour
import com.dressup.core.model.Rating
import org.junit.Assert.assertEquals
import org.junit.Test
import com.dressup.core.model.WearSummary
import java.time.LocalDate

class WardrobeFilterTest {
    private val pants = Category(id = 1, name = "Pants", builtIn = true, hidden = false)
    private val shirt = Category(id = 2, name = "Shirt", builtIn = true, hidden = false)
    private val categories = listOf(pants, shirt)

    private val sportswear = Category(id = 3, name = "Sportswear", builtIn = false, hidden = false)

    private fun garment(
        id: Long,
        category: Category,
        colour: GarmentColour? = null,
        rating: Int? = null,
        liked: Boolean = false,
        also: List<Category> = emptyList(),
    ) = Garment(
        id = id,
        name = null,
        description = null,
        categoryIds = (listOf(category) + also).map { it.id },
        colour = colour,
        photoFileName = null,
        cutoutFileName = null,
        rating = Rating.fromHalfStarsOrNull(rating),
        aspectRatings = emptyMap(),
        review = null,
        liked = liked,
        createdAtMillis = id * 1000,
    )

    private val blueShirt = garment(1, shirt, GarmentColour.BLUE, rating = 6)
    private val blackPants = garment(2, pants, GarmentColour.BLACK, liked = true)
    private val bluePants = garment(3, pants, GarmentColour.BLUE, rating = 9, liked = true, also = listOf(sportswear))
    private val all = listOf(blueShirt, blackPants, bluePants)

    private fun ids(filter: WardrobeFilter) = applyFilter(all, categories, filter).map { it.id }

    @Test
    fun `no filter shows everything, newest first`() {
        assertEquals(listOf(3L, 2L, 1L), ids(WardrobeFilter()))
    }

    @Test
    fun `filters combine`() {
        val filter = WardrobeFilter(categoryIds = setOf(pants.id), colours = setOf(GarmentColour.BLUE))
        assertEquals(listOf(3L), ids(filter))
    }

    @Test
    fun `several values of one filter mean any of them`() {
        assertEquals(listOf(3L, 2L, 1L), ids(WardrobeFilter(colours = setOf(GarmentColour.BLUE, GarmentColour.BLACK))))
    }

    @Test
    fun `a garment matches a category filter through any of its categories`() {
        assertEquals(listOf(3L), ids(WardrobeFilter(categoryIds = setOf(sportswear.id))))
        assertEquals(listOf(3L, 1L), ids(WardrobeFilter(categoryIds = setOf(sportswear.id, shirt.id))))
    }

    @Test
    fun `liked only`() {
        assertEquals(listOf(3L, 2L), ids(WardrobeFilter(likedOnly = true)))
    }

    @Test
    fun `rating sort puts unrated last`() {
        assertEquals(listOf(3L, 1L, 2L), ids(WardrobeFilter(sort = WardrobeSort.RATING)))
    }

    @Test
    fun `category sort follows the category order`() {
        assertEquals(listOf(3L, 2L, 1L), ids(WardrobeFilter(sort = WardrobeSort.CATEGORY)))
    }

    @Test
    fun `oldest first`() {
        assertEquals(listOf(1L, 2L, 3L), ids(WardrobeFilter(sort = WardrobeSort.OLDEST)))
    }

    @Test
    fun `most and least worn, ties broken by the last wear, never worn counts as least`() {
        val wears = mapOf(
            1L to WearSummary(3, LocalDate.of(2026, 11, 1)),
            3L to WearSummary(3, LocalDate.of(2026, 11, 18)),
        )
        fun sorted(sort: WardrobeSort) = applyFilter(all, categories, WardrobeFilter(sort = sort), wears).map { it.id }
        assertEquals(listOf(3L, 1L, 2L), sorted(WardrobeSort.MOST_WORN))
        assertEquals(listOf(2L, 3L, 1L), sorted(WardrobeSort.LEAST_WORN))
    }
}
