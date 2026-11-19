package com.dressup.feature.outfits

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.dressup.core.model.BoardColour
import com.dressup.core.model.OutfitBackground
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

    // Saved, because the system may stop the app while the camera is open.
    var pendingCapture by rememberSaveable { mutableStateOf<String?>(null) }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { taken ->
        val uri = pendingCapture
        pendingCapture = null
        if (taken && uri != null) viewModel.importBackground(Uri.parse(uri))
    }
    val pickPicture = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(viewModel::importBackground)
    }

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
                is OutfitEditorAction.Transform -> viewModel.transform(action.garmentId, action.dx, action.dy, action.zoom, action.rotation)
                is OutfitEditorAction.Resize -> viewModel.resize(action.garmentId, action.scale)
                is OutfitEditorAction.Rotate -> viewModel.rotate(action.garmentId, action.degrees)
                is OutfitEditorAction.SetBackgroundColour -> viewModel.setBackgroundColour(action.colour)
                OutfitEditorAction.BackgroundFailedShown -> viewModel.backgroundFailedShown()
                OutfitEditorAction.BackgroundFromCamera -> {
                    val uri = data.photos.newCaptureUri()
                    pendingCapture = uri.toString()
                    try {
                        takePicture.launch(uri)
                    } catch (_: ActivityNotFoundException) {
                        pendingCapture = null
                    }
                }
                OutfitEditorAction.BackgroundFromGallery ->
                    pickPicture.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
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
    data class Transform(val garmentId: Long, val dx: Float, val dy: Float, val zoom: Float, val rotation: Float) : OutfitEditorAction
    data class Resize(val garmentId: Long, val scale: Float) : OutfitEditorAction
    data class Rotate(val garmentId: Long, val degrees: Float) : OutfitEditorAction
    data class SetBackgroundColour(val colour: BoardColour?) : OutfitEditorAction
    data object BackgroundFromCamera : OutfitEditorAction
    data object BackgroundFromGallery : OutfitEditorAction
    data object BackgroundFailedShown : OutfitEditorAction
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutfitEditorScreen(state: OutfitEditorUiState, onAction: (OutfitEditorAction) -> Unit) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    var choosingBackground by rememberSaveable { mutableStateOf(false) }
    val failedText = stringResource(R.string.outfits_save_failed)
    LaunchedEffect(state.saveFailed) {
        if (state.saveFailed) {
            snackbar.showSnackbar(failedText)
            onAction(OutfitEditorAction.SaveFailedShown)
        }
    }
    val backgroundFailedText = stringResource(R.string.outfits_background_failed)
    LaunchedEffect(state.backgroundFailed) {
        if (state.backgroundFailed) {
            snackbar.showSnackbar(backgroundFailedText)
            onAction(OutfitEditorAction.BackgroundFailedShown)
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
                onTransform = { id, dx, dy, zoom, rotation -> onAction(OutfitEditorAction.Transform(id, dx, dy, zoom, rotation)) },
                background = state.boardBackground,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                stringResource(R.string.outfits_board_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SelectedPieceControls(state, onAction)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FilledTonalButton(onClick = { onAction(OutfitEditorAction.OpenGallery) }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.outfits_pick_garments))
                }
                OutlinedButton(onClick = { choosingBackground = true }, enabled = !state.importingBackground, modifier = Modifier.weight(1f)) {
                    Text(stringResource(if (state.importingBackground) R.string.outfits_background_loading else R.string.outfits_background))
                }
            }
        }
    }
    if (choosingBackground) {
        BackgroundDialog(
            current = state.background,
            onColour = { choosingBackground = false; onAction(OutfitEditorAction.SetBackgroundColour(it)) },
            onCamera = { choosingBackground = false; onAction(OutfitEditorAction.BackgroundFromCamera) },
            onGallery = { choosingBackground = false; onAction(OutfitEditorAction.BackgroundFromGallery) },
            onDismiss = { choosingBackground = false },
        )
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

/**
 * The background: none (the app's own backdrop), one of the palette's colours, or the user's own picture from the
 * camera or the gallery.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BackgroundDialog(
    current: OutfitBackground?,
    onColour: (BoardColour?) -> Unit,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.outfits_background)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Swatch(colour = null, selected = current == null, onClick = { onColour(null) })
                    BoardColour.entries.forEach { colour ->
                        Swatch(colour, selected = (current as? OutfitBackground.Colour)?.colour == colour, onClick = { onColour(colour) })
                    }
                }
                Text(stringResource(R.string.outfits_background_own), style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onCamera) { Text(stringResource(R.string.outfits_background_camera)) }
                    OutlinedButton(onClick = onGallery) { Text(stringResource(R.string.outfits_background_gallery)) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.outfits_cancel)) } },
    )
}

/** A round colour to pick; [colour] null is "no background", drawn as the board's own backdrop with a slash. */
@Composable
private fun Swatch(colour: BoardColour?, selected: Boolean, onClick: () -> Unit) {
    val description = colour?.let { stringResource(it.labelRes) } ?: stringResource(R.string.outfits_background_none)
    val ring = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val fill = colour?.let { Color(it.argb) } ?: MaterialTheme.colorScheme.surfaceVariant
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(fill)
            .border(if (selected) 3.dp else 1.dp, ring, CircleShape)
            .clickable(onClickLabel = description, onClick = onClick)
            .semantics { contentDescription = description; this.selected = selected },
        contentAlignment = Alignment.Center,
    ) {
        if (colour == null) Text("∅", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private val BoardColour.labelRes: Int
    get() = when (this) {
        BoardColour.WHITE -> R.string.outfits_colour_white
        BoardColour.CREAM -> R.string.outfits_colour_cream
        BoardColour.SAND -> R.string.outfits_colour_sand
        BoardColour.BLUSH -> R.string.outfits_colour_blush
        BoardColour.SAGE -> R.string.outfits_colour_sage
        BoardColour.SKY -> R.string.outfits_colour_sky
        BoardColour.MUSTARD -> R.string.outfits_colour_mustard
        BoardColour.TERRACOTTA -> R.string.outfits_colour_terracotta
        BoardColour.SLATE -> R.string.outfits_colour_slate
        BoardColour.PLUM -> R.string.outfits_colour_plum
        BoardColour.CHARCOAL -> R.string.outfits_colour_charcoal
        BoardColour.BLACK -> R.string.outfits_colour_black
    }

/** Size, rotation and remove for the selected garment: the sliders are the way to resize and turn without two fingers. */
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.outfits_rotate), style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = piece.item.rotation,
                onValueChange = { onAction(OutfitEditorAction.Rotate(piece.item.garmentId, it)) },
                valueRange = -180f..180f,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            )
            TextButton(onClick = { onAction(OutfitEditorAction.Rotate(piece.item.garmentId, 0f)) }, enabled = piece.item.rotation != 0f) {
                Text(stringResource(R.string.outfits_straighten))
            }
        }
    }
}
