package com.dressup.core.model

/** A star rating from ½ to 5 stars, stored as a number of half stars (1..10). */
@JvmInline
value class Rating(val halfStars: Int) {
    init {
        require(halfStars in MIN_HALF_STARS..MAX_HALF_STARS) { "Rating must be 1..10 half stars, was $halfStars" }
    }

    val stars: Float get() = halfStars / 2f

    companion object {
        const val MIN_HALF_STARS = 1
        const val MAX_HALF_STARS = 10
        const val STAR_COUNT = 5

        /** A rating from a stored value, or null for "not rated" (0 or anything out of range). */
        fun fromHalfStarsOrNull(halfStars: Int?): Rating? =
            halfStars?.takeIf { it in MIN_HALF_STARS..MAX_HALF_STARS }?.let(::Rating)
    }
}
