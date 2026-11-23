package com.dressup.feature.garment

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dressup.core.data.DressUpData
import com.dressup.core.model.GarmentColour
import com.dressup.core.ui.ColourSwatch
import com.dressup.core.ui.GarmentPhoto
import com.dressup.core.ui.NameDialog
import com.dressup.core.ui.label

/** Adds a garment when [garmentId] is null, otherwise edits it. [onDone] gets the saved garment's id. */
@Composable
fun GarmentEditorRoute(data: DressUpData, garmentId: Long?, onCancel: () -> Unit, onDone: (Long) -> Unit) {
    val viewModel: GarmentEditorViewModel = viewModel(
        key = "editor-${garmentId ?: "new"}",
        factory = viewModelFactory {
            initializer {
                GarmentEditorViewModel(garmentId, createSavedStateHandle(), data.garments, data.categories, data.photos)
            }
        },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.savedGarmentId) { state.savedGarmentId?.let(onDone) }

    // Saved, because the system may stop the app while the camera is open.
    var pendingCapture by rememberSaveable { mutableStateOf<String?>(null) }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { taken ->
        val uri = pendingCapture
        pendingCapture = null
        if (taken && uri != null) viewModel.importPhoto(Uri.parse(uri))
    }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(viewModel::importPhoto)
    }

    GarmentEditorScreen(
        state = state,
        onAction = { action ->
            when (action) {
                EditorAction.Cancel -> onCancel()
                EditorAction.Save -> viewModel.save()
                EditorAction.TakePhoto -> {
                    val uri = viewModel.newCaptureUri()
                    pendingCapture = uri.toString()
                    try {
                        takePicture.launch(uri)
                    } catch (_: ActivityNotFoundException) {
                        pendingCapture = null
                        viewModel.cameraMissing()
                    }
                }
                EditorAction.PickPhoto ->
                    pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                EditorAction.RemovePhoto -> viewModel.removePhoto()
                is EditorAction.ToggleCategory -> viewModel.toggleCategory(action.id)
                is EditorAction.SetName -> viewModel.setName(action.name)
                is EditorAction.SetDescription -> viewModel.setDescription(action.description)
                is EditorAction.ToggleColour -> viewModel.toggleColour(action.colour)
                EditorAction.ClearColours -> viewModel.clearColours()
                EditorAction.NewCategory -> viewModel.openCategoryDialog()
                is EditorAction.AddCategory -> viewModel.addCategory(action.name)
                EditorAction.CloseCategoryDialog -> viewModel.closeCategoryDialog()
                EditorAction.MessageShown -> viewModel.messageShown()
            }
        },
    )
}

sealed interface EditorAction {
    data object Cancel : EditorAction
    data object Save : EditorAction
    data object TakePhoto : EditorAction
    data object PickPhoto : EditorAction
    data object RemovePhoto : EditorAction
    data class ToggleCategory(val id: Long) : EditorAction
    data class SetName(val name: String) : EditorAction
    data class SetDescription(val description: String) : EditorAction
    data class ToggleColour(val colour: GarmentColour) : EditorAction
    data object ClearColours : EditorAction
    data object NewCategory : EditorAction
    data class AddCategory(val name: String) : EditorAction
    data object CloseCategoryDialog : EditorAction
    data object MessageShown : EditorAction
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GarmentEditorScreen(state: GarmentEditorUiState, onAction: (EditorAction) -> Unit) {
    val snackbar = remember { SnackbarHostState() }
    val messageText = state.message?.let { stringResource(it.textRes) }
    LaunchedEffect(state.message) {
        if (messageText != null) {
            snackbar.showSnackbar(messageText)
            onAction(EditorAction.MessageShown)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (state.isNew) R.string.garment_new_title else R.string.garment_edit_title)) },
                navigationIcon = {
                    IconButton(onClick = { onAction(EditorAction.Cancel) }) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.garment_cancel))
                    }
                },
                actions = {
                    TextButton(onClick = { onAction(EditorAction.Save) }, enabled = state.canSave) {
                        Text(stringResource(R.string.garment_save))
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
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            PhotoSection(state, onAction)
            NameSection(state, onAction)
            CategorySection(state, onAction)
            ColourSection(state, onAction)
        }
    }

    state.categoryDialog?.let { dialog ->
        NameDialog(
            title = stringResource(R.string.garment_new_category),
            initialName = "",
            error = dialog.error?.let { stringResource(it.textRes) },
            onConfirm = { onAction(EditorAction.AddCategory(it)) },
            onDismiss = { onAction(EditorAction.CloseCategoryDialog) },
        )
    }
}

@Composable
private fun PhotoSection(state: GarmentEditorUiState, onAction: (EditorAction) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth(0.6f).aspectRatio(3f / 4f).clip(RoundedCornerShape(8.dp))) {
            GarmentPhoto(
                photo = state.photo,
                placeholder = stringResource(R.string.garment_no_photo),
                contentDescription = stringResource(R.string.garment_photo),
                modifier = Modifier.fillMaxSize(),
            )
            if (state.importingPhoto) CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
            FilledTonalButton(onClick = { onAction(EditorAction.TakePhoto) }, enabled = !state.importingPhoto) {
                Text(stringResource(R.string.garment_take_photo))
            }
            OutlinedButton(onClick = { onAction(EditorAction.PickPhoto) }, enabled = !state.importingPhoto) {
                Text(stringResource(R.string.garment_pick_photo))
            }
        }
        if (state.photo != null) {
            TextButton(onClick = { onAction(EditorAction.RemovePhoto) }) { Text(stringResource(R.string.garment_remove_photo)) }
        }
    }
}

@Composable
private fun NameSection(state: GarmentEditorUiState, onAction: (EditorAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = state.name,
            onValueChange = { onAction(EditorAction.SetName(it)) },
            label = { Text(stringResource(R.string.garment_name_optional)) },
            placeholder = { Text(stringResource(R.string.garment_name_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.description,
            onValueChange = { onAction(EditorAction.SetDescription(it)) },
            label = { Text(stringResource(R.string.garment_description_optional)) },
            placeholder = { Text(stringResource(R.string.garment_description_hint)) },
            minLines = 2,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun CategorySection(state: GarmentEditorUiState, onAction: (EditorAction) -> Unit) {
    val mainId = state.selectedCategoryIds.firstOrNull()
    Section(stringResource(R.string.garment_categories_required)) {
        Text(
            stringResource(R.string.garment_categories_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.categories.forEach { category ->
                val selected = category.id in state.selectedCategoryIds
                val isMain = category.id == mainId && state.selectedCategoryIds.size > 1
                FilterChip(
                    selected = selected,
                    onClick = { onAction(EditorAction.ToggleCategory(category.id)) },
                    label = { Text(if (isMain) stringResource(R.string.garment_category_main, category.name) else category.name) },
                    leadingIcon = if (isMain) {
                        { Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else {
                        null
                    },
                )
            }
            AssistChip(
                onClick = { onAction(EditorAction.NewCategory) },
                label = { Text(stringResource(R.string.garment_new_category)) },
                leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) },
            )
        }
    }
}

@Composable
private fun ColourSection(state: GarmentEditorUiState, onAction: (EditorAction) -> Unit) {
    Section(stringResource(R.string.garment_colour_optional)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.colours.isEmpty(),
                onClick = { onAction(EditorAction.ClearColours) },
                label = { Text(stringResource(R.string.garment_no_colour)) },
            )
            GarmentColour.entries.forEach { colour ->
                FilterChip(
                    selected = colour in state.colours,
                    onClick = { onAction(EditorAction.ToggleColour(colour)) },
                    label = { Text(colour.label()) },
                    leadingIcon = { ColourSwatch(colour) },
                )
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        content()
    }
}

private val EditorMessage.textRes: Int
    get() = when (this) {
        EditorMessage.PHOTO_FAILED -> R.string.garment_photo_failed
        EditorMessage.CAMERA_MISSING -> R.string.garment_camera_missing
        EditorMessage.SAVE_FAILED -> R.string.garment_save_failed
    }

private val CategoryNameError.textRes: Int
    get() = when (this) {
        CategoryNameError.BLANK -> R.string.garment_category_blank
        CategoryNameError.DUPLICATE -> R.string.garment_category_duplicate
    }
