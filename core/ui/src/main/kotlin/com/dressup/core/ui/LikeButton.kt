package com.dressup.core.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource

/** The heart, a like independent of the star rating (ARCHITECTURE.md §5). */
@Composable
fun LikeButton(liked: Boolean, onLikedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    IconToggleButton(checked = liked, onCheckedChange = onLikedChange, modifier = modifier) {
        Icon(
            imageVector = if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            contentDescription = stringResource(if (liked) R.string.core_ui_liked else R.string.core_ui_not_liked),
            tint = if (liked) DressUpColors.Rose else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
