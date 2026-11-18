package com.dressup.feature.wardrobe

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dressup.core.data.DressUpData
import com.dressup.core.model.GarmentColour
import com.dressup.core.ui.ColourSwatch
import com.dressup.core.ui.DressUpColors
import com.dressup.core.ui.GarmentPhoto
import com.dressup.core.ui.label

/** The app's start screen. */
@Composable
fun WardrobeRoute(
    data: DressUpData,
    onOpenGarment: (Long) -> Unit,
    onAddGarment: () -> Unit,
    onManageCategories: () -> Unit,
) {
    val viewModel: WardrobeViewModel = viewModel(
        factory = viewModelFactory { initializer { WardrobeViewModel(data.garments, data.categories, data.photos) } },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    WardrobeScreen(
        state = state,
        onAction = { action ->
            when (action) {
                is WardrobeAction.Open -> onOpenGarment(action.garmentId)
                WardrobeAction.Add -> onAddGarment()
                WardrobeAction.ManageCategories -> onManageCategories()
                is WardrobeAction.ToggleCategory -> viewModel.toggleCategory(action.categoryId)
                is WardrobeAction.ToggleColour -> viewModel.toggleColour(action.colour)
                WardrobeAction.ToggleLikedOnly -> viewModel.toggleLikedOnly()
                is WardrobeAction.Sort -> viewModel.setSort(action.sort)
                WardrobeAction.ClearFilters -> viewModel.clearFilters()
            }
        },
    )
}

sealed interface WardrobeAction {
    data class Open(val garmentId: Long) : WardrobeAction
    data object Add : WardrobeAction
    data object ManageCategories : WardrobeAction
    data class ToggleCategory(val categoryId: Long) : WardrobeAction
    data class ToggleColour(val colour: GarmentColour) : WardrobeAction
    data object ToggleLikedOnly : WardrobeAction
    data class Sort(val sort: WardrobeSort) : WardrobeAction
    data object ClearFilters : WardrobeAction
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WardrobeScreen(state: WardrobeUiState, onAction: (WardrobeAction) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.wardrobe_title)) },
                actions = {
                    if (state is WardrobeUiState.Loaded) {
                        SortMenu(state.filter.sort) { onAction(WardrobeAction.Sort(it)) }
                    }
                    IconButton(onClick = { onAction(WardrobeAction.ManageCategories) }) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.wardrobe_manage_categories))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAction(WardrobeAction.Add) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.wardrobe_add_garment))
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (state) {
                WardrobeUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                is WardrobeUiState.Loaded -> LoadedWardrobe(state, onAction)
            }
        }
    }
}

@Composable
private fun LoadedWardrobe(state: WardrobeUiState.Loaded, onAction: (WardrobeAction) -> Unit) {
    if (state.totalCount == 0) {
        Message(stringResource(R.string.wardrobe_empty))
        return
    }
    Column(Modifier.fillMaxSize()) {
        FilterRow(state, onAction)
        if (state.tiles.isEmpty()) {
            Message(stringResource(R.string.wardrobe_no_match)) {
                TextButton(onClick = { onAction(WardrobeAction.ClearFilters) }) {
                    Text(stringResource(R.string.wardrobe_clear_filters))
                }
            }
        } else {
            GarmentGrid(state.tiles) { onAction(WardrobeAction.Open(it)) }
        }
    }
}

@Composable
private fun FilterRow(state: WardrobeUiState.Loaded, onAction: (WardrobeAction) -> Unit) {
    val filter = state.filter
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        item {
            FilterChip(
                selected = filter.likedOnly,
                onClick = { onAction(WardrobeAction.ToggleLikedOnly) },
                label = { Text(stringResource(R.string.wardrobe_filter_liked)) },
                leadingIcon = { Icon(Icons.Filled.Favorite, contentDescription = null, tint = DressUpColors.Rose) },
            )
        }
        items(state.categoriesInUse, key = { "category-${it.id}" }) { category ->
            FilterChip(
                selected = category.id in filter.categoryIds,
                onClick = { onAction(WardrobeAction.ToggleCategory(category.id)) },
                label = { Text(category.name) },
            )
        }
        items(state.coloursInUse, key = { "colour-${it.key}" }) { colour ->
            FilterChip(
                selected = colour in filter.colours,
                onClick = { onAction(WardrobeAction.ToggleColour(colour)) },
                label = { Text(colour.label()) },
                leadingIcon = { ColourSwatch(colour) },
            )
        }
    }
}

@Composable
private fun GarmentGrid(tiles: List<WardrobeTile>, onOpen: (Long) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 104.dp),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 88.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(tiles, key = { it.garment.id }) { tile -> GarmentTile(tile, onClick = { onOpen(tile.garment.id) }) }
    }
}

@Composable
private fun GarmentTile(tile: WardrobeTile, onClick: () -> Unit) {
    // A 2:3 portrait tile, like a poster.
    Box(
        Modifier
            .aspectRatio(2f / 3f)
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
    ) {
        GarmentPhoto(
            photo = tile.photo,
            placeholder = tile.categoryName,
            contentDescription = tile.categoryName,
            modifier = Modifier.fillMaxSize(),
        )
        if (tile.garment.liked) {
            Icon(
                Icons.Filled.Favorite,
                contentDescription = stringResource(R.string.wardrobe_liked),
                tint = DressUpColors.Rose,
                modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp).size(16.dp),
            )
        }
    }
}

@Composable
private fun SortMenu(current: WardrobeSort, onSort: (WardrobeSort) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.AutoMirrored.Filled.List, contentDescription = stringResource(R.string.wardrobe_sort))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            WardrobeSort.entries.forEach { sort ->
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(sort.labelRes),
                            color = if (sort == current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                    },
                    onClick = { onSort(sort); open = false },
                )
            }
        }
    }
}

private val WardrobeSort.labelRes: Int
    get() = when (this) {
        WardrobeSort.NEWEST -> R.string.wardrobe_sort_newest
        WardrobeSort.OLDEST -> R.string.wardrobe_sort_oldest
        WardrobeSort.RATING -> R.string.wardrobe_sort_rating
        WardrobeSort.CATEGORY -> R.string.wardrobe_sort_category
    }

@Composable
private fun Message(text: String, action: @Composable () -> Unit = {}) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        action()
    }
}
