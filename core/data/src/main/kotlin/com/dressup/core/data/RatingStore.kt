package com.dressup.core.data

import com.dressup.core.model.Aspect
import com.dressup.core.model.MAX_REVIEW_LENGTH
import com.dressup.core.model.RatingChange
import com.dressup.core.model.cleanText

/** Rating a garment or an outfit ([com.dressup.core.model.Ratings]): the same for anything that can be rated. */
interface RatingStore<A : Aspect> {
    /** Applies [change] to the thing with [id]. A blank review removes it. */
    suspend fun rate(id: Long, change: RatingChange<A>)
}

/** The columns and rows a [RatingStore] writes, one table's worth; see [RoomRatingStore]. */
internal interface RatingColumns {
    suspend fun setRating(id: Long, halfStars: Int?)

    /** Null half stars removes the aspect's rating. */
    suspend fun setAspectRating(id: Long, aspectKey: String, halfStars: Int?)

    suspend fun setReview(id: Long, review: String?)

    suspend fun setLiked(id: Long, liked: Boolean)
}

internal class RoomRatingStore<A : Aspect>(private val columns: RatingColumns) : RatingStore<A> {
    override suspend fun rate(id: Long, change: RatingChange<A>) = when (change) {
        is RatingChange.Overall -> columns.setRating(id, change.rating?.halfStars)
        is RatingChange.OfAspect -> columns.setAspectRating(id, change.aspect.key, change.rating?.halfStars)
        is RatingChange.Review -> columns.setReview(id, cleanText(change.text, MAX_REVIEW_LENGTH))
        is RatingChange.Liked -> columns.setLiked(id, change.liked)
    }
}
