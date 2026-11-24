package com.dressup.feature.outfits

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dressup.core.data.DressUpData
import com.dressup.core.model.Outfit
import com.dressup.core.ui.OutfitBoard
import com.dressup.core.ui.RatingBar
import com.dressup.core.ui.DressUpColors
import com.dressup.core.ui.EmptyMessage
import com.dressup.core.ui.formatDate

/** The Outfits tab: every outfit as a small collage. */
@Composable
fun OutfitsRoute(
    data: DressUpData,
    profileName: String,
    onOpenOutfit: (Long) -> Unit,
    onNewOutfit: () -> Unit,
    onOpenSettings: () -> Unit,
    bottomBar: @Composable () -> Unit = {},
) {
    val viewModel: OutfitsViewModel = viewModel(
        factory = viewModelFactory {
            initializer { OutfitsViewModel(data.outfits, data.garments, data.categories, data.photos) }
        },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    OutfitsScreen(state, profileName, onOpenOutfit, onNewOutfit, onOpenSettings, bottomBar)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutfitsScreen(
    state: OutfitsUiState,
    profileName: String,
    onOpenOutfit: (Long) -> Unit,
    onNewOutfit: () -> Unit,
    onOpenSettings: () -> Unit,
    bottomBar: @Composable () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.outfits_title))
                        Text(profileName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.outfits_settings))
                    }
                },
            )
        },
        bottomBar = bottomBar,
        floatingActionButton = {
            if (state is OutfitsUiState.Loaded && state.hasGarments) {
                FloatingActionButton(
                    onClick = onNewOutfit,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.outfits_new))
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                state !is OutfitsUiState.Loaded -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                !state.hasGarments -> EmptyMessage(stringResource(R.string.outfits_no_garments))
                state.outfits.isEmpty() -> EmptyMessage(stringResource(R.string.outfits_empty))
                else -> OutfitGrid(state.outfits, onOpenOutfit)
            }
        }
    }
}

@Composable
private fun OutfitGrid(outfits: List<OutfitCard>, onOpen: (Long) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 88.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(outfits, key = { it.outfit.id }) { card ->
            Column(Modifier.clickable { onOpen(card.outfit.id) }, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutfitBoard(card.pieces, Modifier.fillMaxWidth(), background = card.background)
                Text(
                    outfitName(card.outfit),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (card.outfit.ratings.overall != null || card.outfit.ratings.liked) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        card.outfit.ratings.overall?.let { RatingBar(it, starSize = 14.dp) }
                        if (card.outfit.ratings.liked) {
                            Icon(Icons.Filled.Favorite, contentDescription = null, tint = DressUpColors.Rose, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
    }
}

/** The outfit's name, or the date it was made. */
@Composable
internal fun outfitName(outfit: Outfit): String = outfit.name
    ?: stringResource(R.string.outfits_unnamed, formatDate(outfit.createdAtMillis))
