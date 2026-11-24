package com.dressup.core.model

/** Something a garment or an outfit is rated on, besides its overall rating: [GarmentAspect], [OutfitAspect]. */
interface Aspect {
    /** What is stored (in the database and in exports), so it must never change once released. */
    val key: String
}

/**
 * What the user thinks of a garment or an outfit (ARCHITECTURE.md §5, §5.2): the overall stars, the stars per
 * [Aspect] (only the rated ones), a review and a like. Anything new that can be rated gets one of these.
 */
data class Ratings<A : Aspect>(
    val overall: Rating? = null,
    val aspects: Map<A, Rating> = emptyMap(),
    val review: String? = null,
    val liked: Boolean = false,
)

/** One change made on a detail page's rating blocks. A null rating clears it; a blank review removes it. */
sealed interface RatingChange<out A : Aspect> {
    data class Overall(val rating: Rating?) : RatingChange<Nothing>
    data class OfAspect<A : Aspect>(val aspect: A, val rating: Rating?) : RatingChange<A>
    data class Review(val text: String) : RatingChange<Nothing>
    data class Liked(val liked: Boolean) : RatingChange<Nothing>
}
