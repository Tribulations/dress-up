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
import com.dressup.core.model.Rating

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
fun OverallRating(rating: Rating?, liked: Boolean, onRate: (Rating?) -> Unit, onLike: (Boolean) -> Unit) {
    SectionLabel(stringResource(R.string.core_ui_your_rating))
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        RatingBar(rating = rating, starSize = 40.dp, onRatingChange = onRate)
        LikeButton(liked = liked, onLikedChange = onLike)
    }
}

/** "Rate the details": one row of stars per aspect, [aspects] as (label, rating). */
@Composable
fun <A> AspectRatings(aspects: List<Pair<A, Rating?>>, label: @Composable (A) -> String, onRate: (A, Rating?) -> Unit) {
    SectionLabel(stringResource(R.string.core_ui_aspects))
    aspects.forEach { (aspect, rating) ->
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(label(aspect), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            RatingBar(rating = rating, starSize = 28.dp, onRatingChange = { onRate(aspect, it) })
        }
    }
}

/** "Your review": the text, and a button that opens the editor. Saving it empty removes the review. */
@Composable
fun ReviewBlock(review: String?, hint: String, onSave: (String) -> Unit) {
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
            onConfirm = { editing = false; onSave(it) },
            onDismiss = { editing = false },
        )
    }
}
