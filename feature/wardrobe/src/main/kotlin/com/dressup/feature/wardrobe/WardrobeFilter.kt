package com.dressup.feature.wardrobe

import com.dressup.core.model.Category
import com.dressup.core.model.Garment
import com.dressup.core.model.GarmentColour

/** What the wardrobe grid shows. Empty sets mean "no filter". */
data class WardrobeFilter(
    val categoryIds: Set<Long> = emptySet(),
    val colours: Set<GarmentColour> = emptySet(),
    val likedOnly: Boolean = false,
    val sort: WardrobeSort = WardrobeSort.NEWEST,
) {
    val isActive: Boolean get() = categoryIds.isNotEmpty() || colours.isNotEmpty() || likedOnly
}

enum class WardrobeSort { NEWEST, OLDEST, RATING, CATEGORY }

/**
 * Filters and sorts [garments]. A garment matches a category filter through any of its categories; [WardrobeSort.CATEGORY]
 * sorts by the main one, in the order of [categories].
 */
internal fun applyFilter(garments: List<Garment>, categories: List<Category>, filter: WardrobeFilter): List<Garment> {
    val shown = garments.filter { garment ->
        (filter.categoryIds.isEmpty() || garment.categoryIds.any { it in filter.categoryIds }) &&
            (filter.colours.isEmpty() || garment.colour in filter.colours) &&
            (!filter.likedOnly || garment.liked)
    }
    val newestFirst = compareByDescending<Garment> { it.createdAtMillis }.thenByDescending { it.id }
    val comparator = when (filter.sort) {
        WardrobeSort.NEWEST -> newestFirst
        WardrobeSort.OLDEST -> compareBy<Garment> { it.createdAtMillis }.thenBy { it.id }
        // Unrated garments last.
        WardrobeSort.RATING -> compareByDescending<Garment> { it.rating?.halfStars ?: 0 }.then(newestFirst)
        WardrobeSort.CATEGORY -> {
            val position = categories.withIndex().associate { (index, category) -> category.id to index }
            compareBy<Garment> { position[it.categoryIds.firstOrNull()] ?: Int.MAX_VALUE }.then(newestFirst)
        }
    }
    return shown.sortedWith(comparator)
}
