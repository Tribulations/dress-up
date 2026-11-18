package com.dressup.feature.garment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dressup.core.data.CategoryRepository
import com.dressup.core.data.GarmentRepository
import com.dressup.core.data.photo.PhotoStorage
import com.dressup.core.model.Garment
import com.dressup.core.model.Rating
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

sealed interface GarmentDetailUiState {
    data object Loading : GarmentDetailUiState
    data object Gone : GarmentDetailUiState
    data class Loaded(val garment: Garment, val categoryName: String, val photo: File?) : GarmentDetailUiState
}

class GarmentDetailViewModel(
    private val garmentId: Long,
    private val garments: GarmentRepository,
    categories: CategoryRepository,
    photos: PhotoStorage,
) : ViewModel() {
    private val deleted = MutableStateFlow(false)

    val uiState: StateFlow<GarmentDetailUiState> =
        combine(garments.garment(garmentId), categories.categories, deleted) { garment, allCategories, isDeleted ->
            if (garment == null || isDeleted) {
                GarmentDetailUiState.Gone
            } else {
                GarmentDetailUiState.Loaded(
                    garment = garment,
                    categoryName = allCategories.firstOrNull { it.id == garment.categoryId }?.name.orEmpty(),
                    photo = garment.photoFileName?.let(photos::file),
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), GarmentDetailUiState.Loading)

    fun setRating(rating: Rating?) {
        viewModelScope.launch { garments.setRating(garmentId, rating) }
    }

    fun setLiked(liked: Boolean) {
        viewModelScope.launch { garments.setLiked(garmentId, liked) }
    }

    fun delete() {
        deleted.value = true
        viewModelScope.launch { garments.delete(garmentId) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
