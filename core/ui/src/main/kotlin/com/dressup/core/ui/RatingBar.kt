package com.dressup.core.ui

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dressup.core.model.Rating
import kotlin.math.ceil

/**
 * Five stars in half steps (ARCHITECTURE.md §5). Read-only when [onRatingChange] is null.
 *
 * Tap or drag across the stars to rate; tapping the current rating again clears it.
 */
@Composable
fun RatingBar(
    rating: Rating?,
    modifier: Modifier = Modifier,
    starSize: Dp = 20.dp,
    onRatingChange: ((Rating?) -> Unit)? = null,
) {
    var dragHalfStars by remember { mutableStateOf<Int?>(null) }
    val shownHalfStars = dragHalfStars ?: rating?.halfStars ?: 0
    val currentRating by rememberUpdatedState(rating)
    val description = if (rating == null) {
        stringResource(R.string.core_ui_not_rated)
    } else {
        stringResource(R.string.core_ui_rating_description, formatStars(rating))
    }

    val input = if (onRatingChange == null) {
        Modifier
    } else {
        Modifier
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val tapped = halfStarsAt(offset.x, size.width.toFloat())
                    onRatingChange(if (tapped == currentRating?.halfStars) null else Rating(tapped))
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = { dragHalfStars?.let { onRatingChange(Rating(it)) }; dragHalfStars = null },
                    onDragCancel = { dragHalfStars = null },
                ) { change, _ -> dragHalfStars = halfStarsAt(change.position.x, size.width.toFloat()) }
            }
    }

    Row(modifier.then(input).semantics { contentDescription = description }) {
        repeat(Rating.STAR_COUNT) { index ->
            val fill = ((shownHalfStars - index * 2).coerceIn(0, 2)) / 2f
            Star(fill = fill, size = starSize)
        }
    }
}

@Composable
private fun Star(fill: Float, size: Dp) {
    Box(Modifier.size(size)) {
        Icon(Icons.Filled.Star, contentDescription = null, tint = DressUpColors.StarEmpty, modifier = Modifier.size(size))
        if (fill > 0f) {
            Icon(
                Icons.Filled.Star,
                contentDescription = null,
                tint = DressUpColors.Mustard,
                modifier = Modifier
                    .size(size)
                    .drawWithContent { clipRect(right = this.size.width * fill) { this@drawWithContent.drawContent() } },
            )
        }
    }
}

/** The rating (1..10 half stars) for a touch at [x] on a bar [width] wide. */
internal fun halfStarsAt(x: Float, width: Float): Int {
    if (width <= 0f) return Rating.MIN_HALF_STARS
    val halfStars = ceil(x / width * Rating.MAX_HALF_STARS).toInt()
    return halfStars.coerceIn(Rating.MIN_HALF_STARS, Rating.MAX_HALF_STARS)
}

/** "3.5" or "4": the number of stars as the user would say it. */
fun formatStars(rating: Rating): String =
    if (rating.halfStars % 2 == 0) (rating.halfStars / 2).toString() else "%.1f".format(java.util.Locale.ROOT, rating.stars)
