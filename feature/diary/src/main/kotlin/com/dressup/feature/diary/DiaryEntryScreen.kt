package com.dressup.feature.diary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dressup.core.data.DressUpData
import com.dressup.core.ui.DressUpColors
import com.dressup.core.ui.GalleryTile
import com.dressup.core.ui.GarmentPhoto
import com.dressup.core.ui.OutfitBoard
import com.dressup.core.ui.RatingBar
import com.dressup.core.ui.SectionLabel
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun DiaryEntryRoute(
    data: DressUpData,
    entryId: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onOpenOutfit: (Long) -> Unit,
    onOpenGarment: (Long) -> Unit,
) {
    val viewModel: DiaryEntryViewModel = viewModel(
        key = "diary-entry-$entryId",
        factory = viewModelFactory {
            initializer { DiaryEntryViewModel(entryId, data.diary, data.garments, data.categories, data.outfits, data.photos) }
        },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state) { if (state == DiaryEntryUiState.Gone) onBack() }
    DiaryEntryScreen(
        state = state,
        onBack = onBack,
        onEdit = { onEdit(entryId) },
        onDelete = viewModel::delete,
        onOpenOutfit = onOpenOutfit,
        onOpenGarment = onOpenGarment,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryEntryScreen(
    state: DiaryEntryUiState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpenOutfit: (Long) -> Unit,
    onOpenGarment: (Long) -> Unit,
) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.diary_back))
                    }
                },
                actions = {
                    if (state is DiaryEntryUiState.Loaded) {
                        IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.diary_edit)) }
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.diary_delete))
                        }
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (state) {
                is DiaryEntryUiState.Loaded -> EntryDetails(state, onOpenOutfit, onOpenGarment)
                else -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.diary_delete_title)) },
            text = { Text(stringResource(R.string.diary_delete_message)) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) {
                    Text(stringResource(R.string.diary_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.diary_cancel)) } },
        )
    }
}

@Composable
private fun EntryDetails(state: DiaryEntryUiState.Loaded, onOpenOutfit: (Long) -> Unit, onOpenGarment: (Long) -> Unit) {
    val entry = state.entry
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val date = entry.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL))
        // The occasion is the title when there is one ("Christmas Eve"), otherwise the date is.
        val occasion = entry.occasion
        if (occasion != null) {
            Text(date, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(occasion, style = MaterialTheme.typography.headlineMedium)
        } else {
            Text(date, style = MaterialTheme.typography.headlineSmall)
        }
        entry.note?.let { note ->
            SectionLabel(stringResource(R.string.diary_note))
            Text(note, style = MaterialTheme.typography.bodyLarge)
        }
        HorizontalDivider(Modifier.padding(vertical = 4.dp))
        SectionLabel(stringResource(R.string.diary_worn))
        WornHeader(state, onOpenOutfit)
        OutfitBoard(
            pieces = state.worn.pieces,
            onPieceClick = onOpenGarment,
            modifier = Modifier.fillMaxWidth(BOARD_WIDTH).align(Alignment.CenterHorizontally),
        )
        GarmentRow(state.garments, onOpenGarment)
        Box(Modifier.padding(bottom = 24.dp))
    }
}

/** Narrower than the screen, so the garments under it show without scrolling. */
private const val BOARD_WIDTH = 0.72f

/** The outfit's name and rating, as a row that opens the outfit; for single garments, just what they are. */
@Composable
private fun WornHeader(state: DiaryEntryUiState.Loaded, onOpenOutfit: (Long) -> Unit) {
    val outfitId = state.outfitId
    val modifier = if (outfitId != null) Modifier.clickable(onClickLabel = stringResource(R.string.diary_open_outfit)) { onOpenOutfit(outfitId) } else Modifier
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(wornTitle(state.worn), style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                state.worn.rating?.let { RatingBar(it, starSize = 16.dp) }
                if (state.worn.liked) Icon(Icons.Filled.Favorite, contentDescription = null, tint = DressUpColors.Rose, modifier = Modifier.size(16.dp))
            }
        }
        if (outfitId != null) {
            Text(stringResource(R.string.diary_open_outfit), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

/** Every garment worn, each opening its page. */
@Composable
private fun GarmentRow(garments: List<GalleryTile>, onOpen: (Long) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(garments, key = { it.garmentId }) { tile ->
            Column(Modifier.width(76.dp).clickable { onOpen(tile.garmentId) }, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                GarmentPhoto(
                    photo = tile.picture,
                    placeholder = tile.title,
                    contentDescription = tile.title,
                    isCutout = tile.isCutout,
                    cutoutPadding = 4.dp,
                    modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f).clip(RoundedCornerShape(6.dp)),
                )
                Text(tile.title, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
