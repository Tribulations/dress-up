package com.dressup.feature.garment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dressup.core.data.CategoryRepository
import com.dressup.core.data.CutoutResult
import com.dressup.core.data.GarmentRepository
import com.dressup.core.data.photo.PhotoStorage
import com.dressup.core.model.Garment
import com.dressup.core.model.GarmentAspect
import com.dressup.core.model.Rating
import com.dressup.core.ui.garmentSubtitle
import com.dressup.core.ui.garmentTitle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

sealed interface GarmentDetailUiState {
    data object Loading : GarmentDetailUiState
    data object Gone : GarmentDetailUiState
    data class Loaded(
        val garment: Garment,
        /** The garment's own name, or its main category. */
        val title: String,
        /** All its categories, main first. */
        val subtitle: String,
        /** The cut-out when there is one, otherwise the photo. */
        val picture: File?,
        val canCutOut: Boolean,
        val cuttingOut: Boolean,
        val message: DetailMessage?,
    ) : GarmentDetailUiState {
        val isCutout: Boolean get() = garment.cutoutFileName != null
        val hasNameAsTitle: Boolean get() = garment.name != null
    }
}

enum class DetailMessage { NO_PHOTO, NOTHING_FOUND, UNAVAILABLE, FAILED }

private data class DetailStatus(val deleted: Boolean = false, val cuttingOut: Boolean = false, val message: DetailMessage? = null)

class GarmentDetailViewModel(
    private val garmentId: Long,
    private val garments: GarmentRepository,
    categories: CategoryRepository,
    photos: PhotoStorage,
) : ViewModel() {
    private val status = MutableStateFlow(DetailStatus())

    val uiState: StateFlow<GarmentDetailUiState> =
        combine(garments.garment(garmentId), categories.categories, status) { garment, allCategories, s ->
            if (garment == null || s.deleted) {
                GarmentDetailUiState.Gone
            } else {
                val byId = allCategories.associateBy { it.id }
                GarmentDetailUiState.Loaded(
                    garment = garment,
                    title = garmentTitle(garment, byId),
                    subtitle = garmentSubtitle(garment, byId),
                    picture = garment.pictureFileName?.let(photos::file),
                    canCutOut = garments.canCutOut && garment.photoFileName != null,
                    cuttingOut = s.cuttingOut,
                    message = s.message,
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), GarmentDetailUiState.Loading)

    fun setRating(rating: Rating?) {
        viewModelScope.launch { garments.setRating(garmentId, rating) }
    }

    fun setAspectRating(aspect: GarmentAspect, rating: Rating?) {
        viewModelScope.launch { garments.setAspectRating(garmentId, aspect, rating) }
    }

    fun setReview(review: String) {
        viewModelScope.launch { garments.setReview(garmentId, review) }
    }

    fun setLiked(liked: Boolean) {
        viewModelScope.launch { garments.setLiked(garmentId, liked) }
    }

    fun cutOut() {
        if (status.value.cuttingOut) return
        status.update { it.copy(cuttingOut = true) }
        viewModelScope.launch {
            val message = when (garments.cutOut(garmentId)) {
                CutoutResult.DONE -> null
                CutoutResult.NO_PHOTO -> DetailMessage.NO_PHOTO
                CutoutResult.NOTHING_FOUND -> DetailMessage.NOTHING_FOUND
                CutoutResult.UNAVAILABLE -> DetailMessage.UNAVAILABLE
                CutoutResult.FAILED -> DetailMessage.FAILED
            }
            status.update { it.copy(cuttingOut = false, message = message) }
        }
    }

    fun useOriginalPhoto() {
        viewModelScope.launch { garments.removeCutout(garmentId) }
    }

    fun messageShown() = status.update { it.copy(message = null) }

    fun delete() {
        status.update { it.copy(deleted = true) }
        viewModelScope.launch { garments.delete(garmentId) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
