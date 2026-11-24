package com.dressup.feature.diary

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dressup.core.data.DressUpData
import com.dressup.core.ui.GarmentGallery
import com.dressup.core.ui.OutfitBoard
import com.dressup.core.ui.SectionLabel
import com.dressup.core.ui.formatDate
import java.time.LocalDate
import com.dressup.core.ui.EmptyMessage
import com.dressup.core.ui.SnackbarMessage

private const val DAY_MILLIS = 24L * 60 * 60 * 1000

/**
 * Logs a wear, or edits entry [entryId]. A new one can start from an outfit or a garment (the "Log wear" buttons on
 * their pages). [onDone]: saved or cancelled.
 */
@Composable
fun DiaryEditorRoute(data: DressUpData, entryId: Long?, outfitId: Long?, garmentId: Long?, onDone: () -> Unit) {
    val viewModel: DiaryEditorViewModel = viewModel(
        key = "diary-editor-${entryId ?: "new"}-${outfitId ?: ""}-${garmentId ?: ""}",
        factory = viewModelFactory {
            initializer {
                DiaryEditorViewModel(
                    entryId, outfitId, garmentId, createSavedStateHandle(),
                    data.diary, data.garments, data.categories, data.outfits, data.photos,
                )
            }
        },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.done) { if (state.done) onDone() }

    when (state.picker) {
        WornPicker.GARMENTS -> {
            GarmentGallery(
                tiles = state.gallery,
                initiallyPicked = if (state.outfitId == null) state.garmentIds else emptyList(),
                onDone = viewModel::pickGarments,
                onCancel = viewModel::closePicker,
            )
            return
        }
        WornPicker.OUTFIT -> {
            OutfitPicker(state.outfits, onPick = viewModel::chooseOutfit, onCancel = viewModel::closePicker)
            return
        }
        WornPicker.NONE -> Unit
    }
    DiaryEditorScreen(
        state = state,
        onCancel = onDone,
        onSave = viewModel::save,
        onSaveFailedShown = viewModel::saveFailedShown,
        onOpenPicker = viewModel::openPicker,
        onDate = viewModel::setDate,
        onOccasion = viewModel::setOccasion,
        onNote = viewModel::setNote,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryEditorScreen(
    state: DiaryEditorUiState,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    onSaveFailedShown: () -> Unit,
    onOpenPicker: (WornPicker) -> Unit,
    onDate: (LocalDate) -> Unit,
    onOccasion: (String) -> Unit,
    onNote: (String) -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    SnackbarMessage(snackbar, stringResource(R.string.diary_save_failed).takeIf { state.saveFailed }, onSaveFailedShown)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (state.isNew) R.string.diary_new_title else R.string.diary_edit_title)) },
                navigationIcon = {
                    IconButton(onClick = onCancel) { Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.diary_cancel)) }
                },
                actions = {
                    TextButton(onClick = onSave, enabled = state.canSave) { Text(stringResource(R.string.diary_save)) }
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
            Modifier.padding(padding).fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            WornSection(state, onOpenPicker)
            DateSection(state, onDate)
            OccasionSection(state, onOccasion)
            OutlinedTextField(
                value = state.note,
                onValueChange = onNote,
                label = { Text(stringResource(R.string.diary_note_optional)) },
                placeholder = { Text(stringResource(R.string.diary_note_hint)) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
            )
            Spacer(Modifier.padding(bottom = 16.dp))
        }
    }
}

/** What was worn: a small collage and its name, and the two ways to pick it. */
@Composable
private fun WornSection(state: DiaryEditorUiState, onOpenPicker: (WornPicker) -> Unit) {
    SectionLabel(stringResource(R.string.diary_worn))
    val worn = state.worn
    if (worn == null) {
        Text(stringResource(R.string.diary_nothing_picked), style = MaterialTheme.typography.bodyLarge)
    } else {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutfitBoard(worn.pieces, Modifier.width(96.dp), background = worn.background)
            Text(wornTitle(worn), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { onOpenPicker(WornPicker.OUTFIT) }, modifier = Modifier.weight(1f)) {
            Text(stringResource(if (state.outfitId != null) R.string.diary_change_outfit else R.string.diary_choose_outfit))
        }
        OutlinedButton(onClick = { onOpenPicker(WornPicker.GARMENTS) }, modifier = Modifier.weight(1f)) {
            Text(stringResource(if (state.outfitId == null && state.garmentIds.isNotEmpty()) R.string.diary_change_garments else R.string.diary_choose_garments))
        }
    }
}

/** Today and yesterday are one tap; any other past day is in the date picker. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun DateSection(state: DiaryEditorUiState, onDate: (LocalDate) -> Unit) {
    var picking by rememberSaveable { mutableStateOf(false) }
    val yesterday = state.today.minusDays(1)
    SectionLabel(stringResource(R.string.diary_date))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.Center) {
        FilterChip(selected = state.date == state.today, onClick = { onDate(state.today) }, label = { Text(stringResource(R.string.diary_today)) })
        FilterChip(selected = state.date == yesterday, onClick = { onDate(yesterday) }, label = { Text(stringResource(R.string.diary_yesterday)) })
        AssistChip(
            onClick = { picking = true },
            label = { Text(formatDate(state.date)) },
            leadingIcon = { Icon(Icons.Filled.DateRange, contentDescription = stringResource(R.string.diary_pick_date), modifier = Modifier.size(18.dp)) },
        )
    }
    if (picking) {
        val todayMillis = state.today.toEpochDay() * DAY_MILLIS
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.date.toEpochDay() * DAY_MILLIS,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= todayMillis
                override fun isSelectableYear(year: Int) = year <= state.today.year
            },
        )
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { onDate(LocalDate.ofEpochDay(Math.floorDiv(it, DAY_MILLIS))) }
                    picking = false
                }) { Text(stringResource(R.string.diary_ok)) }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text(stringResource(R.string.diary_cancel)) } },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

/** A free text, with the occasions used before as chips, so the same occasion keeps the same name. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OccasionSection(state: DiaryEditorUiState, onOccasion: (String) -> Unit) {
    OutlinedTextField(
        value = state.occasion,
        onValueChange = onOccasion,
        label = { Text(stringResource(R.string.diary_occasion_optional)) },
        placeholder = { Text(stringResource(R.string.diary_occasion_hint)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        modifier = Modifier.fillMaxWidth(),
    )
    if (state.suggestions.isNotEmpty()) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.suggestions.forEach { occasion ->
                AssistChip(onClick = { onOccasion(occasion) }, label = { Text(occasion, maxLines = 1, overflow = TextOverflow.Ellipsis) })
            }
        }
    }
}

/** Every outfit as a small collage; tapping one picks it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OutfitPicker(outfits: List<OutfitChoice>, onPick: (Long) -> Unit, onCancel: () -> Unit) {
    BackHandler(onBack = onCancel)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.diary_pick_outfit_title)) },
                navigationIcon = {
                    IconButton(onClick = onCancel) { Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.diary_cancel)) }
                },
            )
        },
    ) { padding ->
        if (outfits.isEmpty()) {
            Box(Modifier.padding(padding)) { EmptyMessage(stringResource(R.string.diary_no_outfits)) }
            return@Scaffold
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 140.dp),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            items(outfits, key = { it.id }) { outfit ->
                Column(Modifier.clickable { onPick(outfit.id) }, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutfitBoard(outfit.pieces, Modifier.fillMaxWidth(), background = outfit.background)
                    Text(
                        outfit.name ?: stringResource(R.string.diary_outfit),
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
