package com.dressup.feature.garment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dressup.core.data.CategoryRepository
import com.dressup.core.data.CutoutResult
import com.dressup.core.data.DiaryRepository
import com.dressup.core.data.GarmentRepository
import com.dressup.core.data.photo.PhotoStorage
import com.dressup.core.model.DiaryEntry
import com.dressup.core.model.Garment
import com.dressup.core.model.GarmentAspect
import com.dressup.core.model.RatingChange
import com.dressup.core.model.wearsOfGarment
import com.dressup.core.ui.garmentSubtitle
import com.dressup.core.ui.garmentTitle
import com.dressup.core.ui.WhileUiSubscribed
import kotlinx.coroutines.flow.MutableStateFlow
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
        /** The diary entries it was worn in, newest first. */
        val wears: List<DiaryEntry> = emptyList(),
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
    diary: DiaryRepository,
) : ViewModel() {
    private val status = MutableStateFlow(DetailStatus())

    val uiState: StateFlow<GarmentDetailUiState> =
        combine(garments.garment(garmentId), categories.categories, status, diary.entries) { garment, allCategories, s, entries ->
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
                    wears = entries.wearsOfGarment(garmentId),
                )
            }
        }.stateIn(viewModelScope, WhileUiSubscribed, GarmentDetailUiState.Loading)

    fun rate(change: RatingChange<GarmentAspect>) {
        viewModelScope.launch { garments.rate(garmentId, change) }
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
}
