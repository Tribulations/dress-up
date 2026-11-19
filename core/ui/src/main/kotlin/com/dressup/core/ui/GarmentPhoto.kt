package com.dressup.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import java.io.File

/**
 * A garment's picture filling [modifier]'s size, or [placeholder] text when there is none.
 *
 * A photo is cropped to fill. A cut-out ([isCutout]) is zoomed to fill the frame while staying whole (scaled to fit,
 * with only [cutoutPadding] around it, so no sleeve or toe is cut off), on a soft backdrop of the app's own, so
 * cut-out garments look alike in the grid; without [cutoutBackdrop] it floats on whatever is behind it (the outfit
 * board).
 */
@Composable
fun GarmentPhoto(
    photo: File?,
    placeholder: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    isCutout: Boolean = false,
    cutoutPadding: Dp = 4.dp,
    cutoutBackdrop: Boolean = true,
) {
    val background = if (isCutout && photo != null && !cutoutBackdrop) {
        Modifier
    } else if (isCutout && photo != null) {
        Modifier.background(
            Brush.verticalGradient(listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surfaceContainerHighest)),
        )
    } else {
        Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
    }
    Box(modifier.then(background), contentAlignment = Alignment.Center) {
        when {
            photo == null -> Text(
                text = placeholder,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(8.dp),
            )
            isCutout -> AsyncImage(
                model = photo,
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(cutoutPadding),
            )
            else -> AsyncImage(
                model = photo,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}
