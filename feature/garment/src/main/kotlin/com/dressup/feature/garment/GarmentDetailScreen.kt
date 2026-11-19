package com.dressup.feature.garment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dressup.core.data.DressUpData
import com.dressup.core.model.GarmentAspect
import com.dressup.core.model.Rating
import com.dressup.core.ui.ColourSwatch
import com.dressup.core.ui.GarmentPhoto
import com.dressup.core.ui.AspectRatings
import com.dressup.core.ui.OverallRating
import com.dressup.core.ui.ReviewBlock
import com.dressup.core.ui.WearHistory
import com.dressup.core.ui.label
import java.text.DateFormat
import java.util.Date

@Composable
fun GarmentDetailRoute(
    data: DressUpData,
    garmentId: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onLogWear: (garmentId: Long) -> Unit,
    onOpenEntry: (entryId: Long) -> Unit,
) {
    val viewModel: GarmentDetailViewModel = viewModel(
        key = "garment-$garmentId",
        factory = viewModelFactory {
            initializer { GarmentDetailViewModel(garmentId, data.garments, data.categories, data.photos, data.diary) }
        },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state) { if (state == GarmentDetailUiState.Gone) onBack() }
    GarmentDetailScreen(
        state = state,
        onBack = onBack,
        onAction = { action ->
            when (action) {
                DetailAction.Edit -> onEdit(garmentId)
                DetailAction.Delete -> viewModel.delete()
                is DetailAction.Rate -> viewModel.setRating(action.rating)
                is DetailAction.RateAspect -> viewModel.setAspectRating(action.aspect, action.rating)
                is DetailAction.Like -> viewModel.setLiked(action.liked)
                is DetailAction.SaveReview -> viewModel.setReview(action.review)
                DetailAction.CutOut -> viewModel.cutOut()
                DetailAction.UseOriginalPhoto -> viewModel.useOriginalPhoto()
                DetailAction.MessageShown -> viewModel.messageShown()
                DetailAction.LogWear -> onLogWear(garmentId)
                is DetailAction.OpenEntry -> onOpenEntry(action.entryId)
            }
        },
    )
}

sealed interface DetailAction {
    data object Edit : DetailAction
    data object Delete : DetailAction
    data class Rate(val rating: Rating?) : DetailAction
    data class RateAspect(val aspect: GarmentAspect, val rating: Rating?) : DetailAction
    data class Like(val liked: Boolean) : DetailAction
    data class SaveReview(val review: String) : DetailAction
    data object CutOut : DetailAction
    data object UseOriginalPhoto : DetailAction
    data object MessageShown : DetailAction
    data object LogWear : DetailAction
    data class OpenEntry(val entryId: Long) : DetailAction
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GarmentDetailScreen(state: GarmentDetailUiState, onBack: () -> Unit, onAction: (DetailAction) -> Unit) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val message = (state as? GarmentDetailUiState.Loaded)?.message
    val messageText = message?.let { stringResource(it.textRes) }
    LaunchedEffect(message) {
        if (messageText != null) {
            snackbar.showSnackbar(messageText)
            onAction(DetailAction.MessageShown)
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.garment_back))
                    }
                },
                actions = {
                    if (state is GarmentDetailUiState.Loaded) {
                        IconButton(onClick = { onAction(DetailAction.Edit) }) {
                            Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.garment_edit))
                        }
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.garment_delete))
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (state) {
                is GarmentDetailUiState.Loaded -> GarmentDetails(state, onAction)
                else -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.garment_delete_title)) },
            text = { Text(stringResource(R.string.garment_delete_message)) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onAction(DetailAction.Delete) }) {
                    Text(stringResource(R.string.garment_delete_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.garment_cancel)) }
            },
        )
    }
}

@Composable
private fun GarmentDetails(state: GarmentDetailUiState.Loaded, onAction: (DetailAction) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth().aspectRatio(3f / 4f)) {
            GarmentPhoto(
                photo = state.picture,
                placeholder = stringResource(R.string.garment_no_photo),
                contentDescription = state.title,
                isCutout = state.isCutout,
                cutoutPadding = 12.dp,
                modifier = Modifier.fillMaxSize(),
            )
            if (state.cuttingOut) CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CutoutButton(state, onAction)
            Heading(state)
            state.garment.description?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
            Text(
                stringResource(R.string.garment_added_on, formatDate(state.garment.createdAtMillis)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            OverallRating(
                rating = state.garment.rating,
                liked = state.garment.liked,
                onRate = { onAction(DetailAction.Rate(it)) },
                onLike = { onAction(DetailAction.Like(it)) },
            )
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            WearHistory(
                wears = state.wears,
                onLogWear = { onAction(DetailAction.LogWear) },
                onOpenEntry = { onAction(DetailAction.OpenEntry(it)) },
            )
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            AspectRatings(
                aspects = GarmentAspect.entries.map { it to state.garment.aspectRatings[it] },
                label = { it.label() },
                onRate = { aspect, rating -> onAction(DetailAction.RateAspect(aspect, rating)) },
            )
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            ReviewBlock(
                review = state.garment.review,
                hint = stringResource(R.string.garment_review_hint),
                onSave = { onAction(DetailAction.SaveReview(it)) },
            )
        }
    }
}

@Composable
private fun CutoutButton(state: GarmentDetailUiState.Loaded, onAction: (DetailAction) -> Unit) {
    when {
        state.isCutout -> OutlinedButton(onClick = { onAction(DetailAction.UseOriginalPhoto) }, enabled = !state.cuttingOut) {
            Text(stringResource(R.string.garment_show_original))
        }
        state.canCutOut -> FilledTonalButton(onClick = { onAction(DetailAction.CutOut) }, enabled = !state.cuttingOut) {
            Text(stringResource(if (state.cuttingOut) R.string.garment_removing_background else R.string.garment_remove_background))
        }
    }
}

/** The name on top, its categories (and colour) underneath (ARCHITECTURE.md §8). */
@Composable
private fun Heading(state: GarmentDetailUiState.Loaded) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(state.title, style = MaterialTheme.typography.headlineMedium)
        val colour = state.garment.colour
        val showCategories = state.hasNameAsTitle || state.garment.categoryIds.size > 1
        if (showCategories || colour != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showCategories) {
                    Text(state.subtitle, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
                if (colour != null) {
                    if (showCategories) Spacer(Modifier.width(12.dp))
                    ColourSwatch(colour)
                    Spacer(Modifier.width(6.dp))
                    Text(colour.label(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private val DetailMessage.textRes: Int
    get() = when (this) {
        DetailMessage.NO_PHOTO -> R.string.garment_cutout_no_photo
        DetailMessage.NOTHING_FOUND -> R.string.garment_cutout_nothing_found
        DetailMessage.UNAVAILABLE -> R.string.garment_cutout_unavailable
        DetailMessage.FAILED -> R.string.garment_cutout_failed
    }

private fun formatDate(millis: Long): String = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(millis))
