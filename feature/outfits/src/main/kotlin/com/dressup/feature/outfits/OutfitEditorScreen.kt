package com.dressup.feature.outfits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dressup.core.data.DressUpData
import com.dressup.core.model.OutfitItem
import com.dressup.core.ui.GarmentGallery
import com.dressup.core.ui.OutfitBoard

/**
 * Makes a new outfit when [outfitId] is null, otherwise edits it. [onDone]: saved (with the outfit's id), or deleted or
 * cancelled (null).
 */
@Composable
fun OutfitEditorRoute(data: DressUpData, outfitId: Long?, onDone: (savedOutfitId: Long?) -> Unit) {
    val viewModel: OutfitEditorViewModel = viewModel(
        key = "outfit-${outfitId ?: "new"}",
        factory = viewModelFactory {
            initializer {
                OutfitEditorViewModel(outfitId, createSavedStateHandle(), data.outfits, data.garments, data.categories, data.photos)
            }
        },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.done) { if (state.done) onDone(state.savedId) }

    if (state.galleryOpen) {
        GarmentGallery(
            tiles = state.gallery,
            initiallyPicked = state.pieces.map { it.item.garmentId },
            onDone = viewModel::pickGarments,
            // Closing the gallery of a brand-new, still empty outfit means it isn't wanted after all.
            onCancel = { if (state.isNew && state.pieces.isEmpty()) onDone(null) else viewModel.closeGallery() },
        )
        return
    }
    OutfitEditorScreen(
        state = state,
        onAction = { action ->
            when (action) {
                OutfitEditorAction.Cancel -> onDone(null)
                OutfitEditorAction.Save -> viewModel.save()
                OutfitEditorAction.Delete -> viewModel.delete()
                OutfitEditorAction.OpenGallery -> viewModel.openGallery()
                OutfitEditorAction.RemoveSelected -> viewModel.removeSelected()
                OutfitEditorAction.SaveFailedShown -> viewModel.saveFailedShown()
                is OutfitEditorAction.SetName -> viewModel.setName(action.name)
                is OutfitEditorAction.Select -> viewModel.select(action.garmentId)
                is OutfitEditorAction.Transform -> viewModel.transform(action.garmentId, action.dx, action.dy, action.zoom)
                is OutfitEditorAction.Resize -> viewModel.resize(action.garmentId, action.scale)
            }
        },
    )
}

sealed interface OutfitEditorAction {
    data object Cancel : OutfitEditorAction
    data object Save : OutfitEditorAction
    data object Delete : OutfitEditorAction
    data object OpenGallery : OutfitEditorAction
    data object RemoveSelected : OutfitEditorAction
    data object SaveFailedShown : OutfitEditorAction
    data class SetName(val name: String) : OutfitEditorAction
    data class Select(val garmentId: Long?) : OutfitEditorAction
    data class Transform(val garmentId: Long, val dx: Float, val dy: Float, val zoom: Float) : OutfitEditorAction
    data class Resize(val garmentId: Long, val scale: Float) : OutfitEditorAction
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutfitEditorScreen(state: OutfitEditorUiState, onAction: (OutfitEditorAction) -> Unit) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val failedText = stringResource(R.string.outfits_save_failed)
    LaunchedEffect(state.saveFailed) {
        if (state.saveFailed) {
            snackbar.showSnackbar(failedText)
            onAction(OutfitEditorAction.SaveFailedShown)
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (state.isNew) R.string.outfits_new_title else R.string.outfits_edit_title)) },
                navigationIcon = {
                    IconButton(onClick = { onAction(OutfitEditorAction.Cancel) }) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.outfits_cancel))
                    }
                },
                actions = {
                    if (!state.isNew) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.outfits_delete))
                        }
                    }
                    TextButton(onClick = { onAction(OutfitEditorAction.Save) }, enabled = state.canSave) {
                        Text(stringResource(R.string.outfits_save))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        if (!state.loaded) {
            Box(Modifier.padding(padding).fillMaxSize()) { CircularProgressIndicator(Modifier.align(Alignment.Center)) }
            return@Scaffold
        }
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = { onAction(OutfitEditorAction.SetName(it)) },
                label = { Text(stringResource(R.string.outfits_name_optional)) },
                placeholder = { Text(stringResource(R.string.outfits_name_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            OutfitBoard(
                pieces = state.pieces,
                selectedGarmentId = state.selectedGarmentId,
                onSelect = { onAction(OutfitEditorAction.Select(it)) },
                onTransform = { id, dx, dy, zoom -> onAction(OutfitEditorAction.Transform(id, dx, dy, zoom)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                stringResource(R.string.outfits_board_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SelectedPieceControls(state, onAction)
            FilledTonalButton(onClick = { onAction(OutfitEditorAction.OpenGallery) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.outfits_pick_garments))
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.outfits_delete_title)) },
            text = { Text(stringResource(R.string.outfits_delete_message)) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onAction(OutfitEditorAction.Delete) }) {
                    Text(stringResource(R.string.outfits_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.outfits_cancel)) } },
        )
    }
}

/** Size and remove for the selected garment: the slider is the way to resize without pinching. */
@Composable
private fun SelectedPieceControls(state: OutfitEditorUiState, onAction: (OutfitEditorAction) -> Unit) {
    val piece = state.selectedPiece ?: return
    Column {
        Text(piece.title, style = MaterialTheme.typography.titleSmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.outfits_size), style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = piece.item.scale,
                onValueChange = { onAction(OutfitEditorAction.Resize(piece.item.garmentId, it)) },
                valueRange = OutfitItem.MIN_SCALE..OutfitItem.MAX_SCALE,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            )
            OutlinedButton(onClick = { onAction(OutfitEditorAction.RemoveSelected) }) {
                Text(stringResource(R.string.outfits_remove_piece))
            }
        }
    }
}
