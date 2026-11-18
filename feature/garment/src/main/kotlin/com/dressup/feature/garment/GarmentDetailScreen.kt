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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dressup.core.data.DressUpData
import com.dressup.core.model.Rating
import com.dressup.core.ui.ColourSwatch
import com.dressup.core.ui.GarmentPhoto
import com.dressup.core.ui.LikeButton
import com.dressup.core.ui.RatingBar
import com.dressup.core.ui.label
import java.text.DateFormat
import java.util.Date

@Composable
fun GarmentDetailRoute(data: DressUpData, garmentId: Long, onBack: () -> Unit, onEdit: (Long) -> Unit) {
    val viewModel: GarmentDetailViewModel = viewModel(
        key = "garment-$garmentId",
        factory = viewModelFactory {
            initializer { GarmentDetailViewModel(garmentId, data.garments, data.categories, data.photos) }
        },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state) { if (state == GarmentDetailUiState.Gone) onBack() }
    GarmentDetailScreen(
        state = state,
        onBack = onBack,
        onEdit = { onEdit(garmentId) },
        onDelete = viewModel::delete,
        onRatingChange = viewModel::setRating,
        onLikedChange = viewModel::setLiked,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GarmentDetailScreen(
    state: GarmentDetailUiState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRatingChange: (Rating?) -> Unit,
    onLikedChange: (Boolean) -> Unit,
) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
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
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.garment_edit))
                        }
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.garment_delete))
                        }
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (state) {
                is GarmentDetailUiState.Loaded -> GarmentDetails(state, onRatingChange, onLikedChange)
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
                TextButton(onClick = { confirmDelete = false; onDelete() }) {
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
private fun GarmentDetails(
    state: GarmentDetailUiState.Loaded,
    onRatingChange: (Rating?) -> Unit,
    onLikedChange: (Boolean) -> Unit,
) {
    val garment = state.garment
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        GarmentPhoto(
            photo = state.photo,
            placeholder = stringResource(R.string.garment_no_photo),
            contentDescription = state.categoryName,
            modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f),
        )
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(state.categoryName, style = MaterialTheme.typography.headlineMedium)
            garment.colour?.let { colour ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ColourSwatch(colour)
                    Text(colour.label(), style = MaterialTheme.typography.bodyLarge)
                }
            }
            Text(
                stringResource(R.string.garment_added_on, formatDate(garment.createdAtMillis)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.garment_your_rating),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                RatingBar(rating = garment.rating, starSize = 40.dp, onRatingChange = onRatingChange)
                LikeButton(liked = garment.liked, onLikedChange = onLikedChange)
            }
        }
    }
}

private fun formatDate(millis: Long): String = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(millis))
