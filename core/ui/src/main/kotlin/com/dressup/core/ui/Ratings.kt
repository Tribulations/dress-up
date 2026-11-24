package com.dressup.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dressup.core.model.MAX_REVIEW_LENGTH
import com.dressup.core.model.Aspect
import com.dressup.core.model.RatingChange
import com.dressup.core.model.Ratings

/*
 * The rating blocks of the garment and outfit pages (ARCHITECTURE.md §8): the same look and behaviour for both.
 * Meant for a Column with spacing between its children.
 */

/** A small grey heading over a block of a detail page. */
@Composable
fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** "Your rating": big stars and the heart. */
@Composable
fun <A : Aspect> OverallRating(ratings: Ratings<A>, onChange: (RatingChange<A>) -> Unit) {
    SectionLabel(stringResource(R.string.core_ui_your_rating))
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        RatingBar(rating = ratings.overall, starSize = 40.dp, onRatingChange = { onChange(RatingChange.Overall(it)) })
        LikeButton(liked = ratings.liked, onLikedChange = { onChange(RatingChange.Liked(it)) })
    }
}

/** "Rate the details": one row of stars for each of [aspects], in that order. */
@Composable
fun <A : Aspect> AspectRatings(
    ratings: Ratings<A>,
    aspects: List<A>,
    label: @Composable (A) -> String,
    onChange: (RatingChange<A>) -> Unit,
) {
    SectionLabel(stringResource(R.string.core_ui_aspects))
    aspects.forEach { aspect ->
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(label(aspect), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            RatingBar(rating = ratings.aspects[aspect], starSize = 28.dp, onRatingChange = { onChange(RatingChange.OfAspect(aspect, it)) })
        }
    }
}

/** "Your review": the text, and a button that opens the editor. Saving it empty removes the review. */
@Composable
fun <A : Aspect> ReviewBlock(ratings: Ratings<A>, hint: String, onChange: (RatingChange<A>) -> Unit) {
    val review = ratings.review
    var editing by rememberSaveable { mutableStateOf(false) }
    SectionLabel(stringResource(R.string.core_ui_review))
    if (review != null) Text(review, style = MaterialTheme.typography.bodyLarge)
    TextButton(onClick = { editing = true }) {
        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(if (review == null) R.string.core_ui_review_write else R.string.core_ui_review_edit))
    }
    if (editing) {
        TextDialog(
            title = stringResource(R.string.core_ui_review),
            initialText = review.orEmpty(),
            maxLength = MAX_REVIEW_LENGTH,
            placeholder = hint,
            onConfirm = { editing = false; onChange(RatingChange.Review(it)) },
            onDismiss = { editing = false },
        )
    }
}
