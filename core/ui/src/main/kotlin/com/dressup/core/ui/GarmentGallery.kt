package com.dressup.core.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.io.File

/** One garment in a [GarmentGallery]. */
data class GalleryTile(val garmentId: Long, val title: String, val picture: File?, val isCutout: Boolean)

/**
 * A gallery of every garment's picture (cut-outs where there are any) to pick garments from, for an outfit or a diary
 * entry. Tap to pick or unpick; [onDone] gets the picked ones in the order picked.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GarmentGallery(tiles: List<GalleryTile>, initiallyPicked: List<Long>, onDone: (List<Long>) -> Unit, onCancel: () -> Unit) {
    var picked by rememberSaveable { mutableStateOf(initiallyPicked.toLongArray()) }
    BackHandler(onBack = onCancel)
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (picked.isEmpty()) {
                            stringResource(R.string.core_ui_gallery_title)
                        } else {
                            pluralStringResource(R.plurals.core_ui_gallery_picked, picked.size, picked.size)
                        },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.core_ui_cancel))
                    }
                },
                actions = {
                    TextButton(onClick = { onDone(picked.toList()) }, enabled = picked.isNotEmpty()) {
                        Text(stringResource(R.string.core_ui_gallery_done))
                    }
                },
            )
        },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 96.dp),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            items(tiles, key = { it.garmentId }) { tile ->
                val isPicked = tile.garmentId in picked
                GalleryItem(tile, isPicked) {
                    picked = if (isPicked) picked.filter { it != tile.garmentId }.toLongArray() else picked + tile.garmentId
                }
            }
        }
    }
}

@Composable
private fun GalleryItem(tile: GalleryTile, picked: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(6.dp)
    val outline = if (picked) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, shape) else Modifier
    Box(
        Modifier
            .aspectRatio(2f / 3f)
            .clip(shape)
            .then(outline)
            .clickable(onClick = onClick)
            .semantics { selected = picked },
    ) {
        GarmentPhoto(
            photo = tile.picture,
            placeholder = tile.title,
            contentDescription = tile.title,
            isCutout = tile.isCutout,
            cutoutPadding = 3.dp,
            modifier = Modifier.fillMaxSize(),
        )
        if (picked) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(22.dp).clip(CircleShape),
            )
        }
    }
}
