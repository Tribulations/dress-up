package com.dressup.feature.outfits

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.dressup.core.model.OutfitAspect
import com.dressup.core.model.RatingChange
import com.dressup.core.ui.AspectRatings
import com.dressup.core.ui.GalleryTile
import com.dressup.core.ui.GarmentPhoto
import com.dressup.core.ui.OutfitBoard
import com.dressup.core.ui.OverallRating
import com.dressup.core.ui.ReviewBlock
import com.dressup.core.ui.SectionLabel
import com.dressup.core.ui.WearHistory
import com.dressup.core.ui.label
import com.dressup.core.ui.ConfirmDeleteDialog

@Composable
fun OutfitDetailRoute(
    data: DressUpData,
    outfitId: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onOpenGarment: (Long) -> Unit,
    onLogWear: (outfitId: Long) -> Unit,
    onOpenEntry: (entryId: Long) -> Unit,
) {
    val viewModel: OutfitDetailViewModel = viewModel(
        key = "outfit-detail-$outfitId",
        factory = viewModelFactory {
            initializer { OutfitDetailViewModel(outfitId, data.outfits, data.garments, data.categories, data.photos, data.diary) }
        },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state) { if (state == OutfitDetailUiState.Gone) onBack() }
    OutfitDetailScreen(
        state = state,
        onBack = onBack,
        onAction = { action ->
            when (action) {
                OutfitDetailAction.Edit -> onEdit(outfitId)
                OutfitDetailAction.Delete -> viewModel.delete()
                OutfitDetailAction.LogWear -> onLogWear(outfitId)
                is OutfitDetailAction.OpenGarment -> onOpenGarment(action.garmentId)
                is OutfitDetailAction.OpenEntry -> onOpenEntry(action.entryId)
                is OutfitDetailAction.Rate -> viewModel.rate(action.change)
            }
        },
    )
}

sealed interface OutfitDetailAction {
    data object Edit : OutfitDetailAction
    data object Delete : OutfitDetailAction
    data object LogWear : OutfitDetailAction
    data class OpenGarment(val garmentId: Long) : OutfitDetailAction
    data class OpenEntry(val entryId: Long) : OutfitDetailAction
    data class Rate(val change: RatingChange<OutfitAspect>) : OutfitDetailAction
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutfitDetailScreen(state: OutfitDetailUiState, onBack: () -> Unit, onAction: (OutfitDetailAction) -> Unit) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.outfits_back))
                    }
                },
                actions = {
                    if (state is OutfitDetailUiState.Loaded) {
                        IconButton(onClick = { onAction(OutfitDetailAction.Edit) }) {
                            Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.outfits_edit))
                        }
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.outfits_delete))
                        }
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (state) {
                is OutfitDetailUiState.Loaded -> OutfitDetails(state, onAction)
                else -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        }
    }
    if (confirmDelete) {
        ConfirmDeleteDialog(
            title = stringResource(R.string.outfits_delete_title),
            message = stringResource(R.string.outfits_delete_message),
            onConfirm = { confirmDelete = false; onAction(OutfitDetailAction.Delete) },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun OutfitDetails(state: OutfitDetailUiState.Loaded, onAction: (OutfitDetailAction) -> Unit) {
    val outfit = state.outfit
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        OutfitBoard(
            pieces = state.pieces,
            onPieceClick = { onAction(OutfitDetailAction.OpenGarment(it)) },
            background = state.background,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(outfitName(outfit), style = MaterialTheme.typography.headlineMedium)
            GarmentStrip(state.garments, onOpen = { onAction(OutfitDetailAction.OpenGarment(it)) })
            Spacer(Modifier.height(4.dp))
            val rate = { change: RatingChange<OutfitAspect> -> onAction(OutfitDetailAction.Rate(change)) }
            OverallRating(outfit.ratings, onChange = rate)
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            WearHistory(
                wears = state.wears,
                onLogWear = { onAction(OutfitDetailAction.LogWear) },
                onOpenEntry = { onAction(OutfitDetailAction.OpenEntry(it)) },
            )
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            AspectRatings(outfit.ratings, OutfitAspect.entries, label = { it.label() }, onChange = rate)
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            ReviewBlock(outfit.ratings, hint = stringResource(R.string.outfits_review_hint), onChange = rate)
        }
    }
}

/** The outfit's garments in a row, each opening its garment page. */
@Composable
internal fun GarmentStrip(garments: List<GalleryTile>, onOpen: (Long) -> Unit) {
    if (garments.isEmpty()) return
    SectionLabel(stringResource(R.string.outfits_garments, garments.size))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(0.dp)) {
        items(garments, key = { it.garmentId }) { tile ->
            Column(Modifier.width(76.dp).clickable { onOpen(tile.garmentId) }, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                GarmentPhoto(
                    photo = tile.picture,
                    placeholder = tile.title,
                    contentDescription = tile.title,
                    isCutout = tile.isCutout,
                    cutoutPadding = 2.dp,
                    modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f).clip(RoundedCornerShape(6.dp)),
                )
                Text(tile.title, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
