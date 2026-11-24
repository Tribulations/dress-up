package com.dressup.feature.outfits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dressup.core.data.CategoryRepository
import com.dressup.core.data.DiaryRepository
import com.dressup.core.data.GarmentRepository
import com.dressup.core.data.OutfitRepository
import com.dressup.core.data.photo.PhotoStorage
import com.dressup.core.model.DiaryEntry
import com.dressup.core.model.Outfit
import com.dressup.core.model.OutfitAspect
import com.dressup.core.model.RatingChange
import com.dressup.core.model.wearsOfOutfit
import com.dressup.core.ui.BoardBackground
import com.dressup.core.ui.BoardPiece
import com.dressup.core.ui.GalleryTile
import com.dressup.core.ui.PieceFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface OutfitDetailUiState {
    data object Loading : OutfitDetailUiState
    data object Gone : OutfitDetailUiState
    data class Loaded(
        val outfit: Outfit,
        val pieces: List<BoardPiece>,
        val background: BoardBackground?,
        /** Its garments, in board order, to open one. */
        val garments: List<GalleryTile>,
        /** The diary entries it was worn in, newest first. */
        val wears: List<DiaryEntry>,
    ) : OutfitDetailUiState
}

/** An outfit's page: the collage, its garments, ratings, review and wear history (ARCHITECTURE.md §7.6). */
class OutfitDetailViewModel(
    private val outfitId: Long,
    private val outfits: OutfitRepository,
    garments: GarmentRepository,
    categories: CategoryRepository,
    photos: PhotoStorage,
    diary: DiaryRepository,
) : ViewModel() {
    private val deleted = MutableStateFlow(false)

    val uiState: StateFlow<OutfitDetailUiState> =
        combine(outfits.outfit(outfitId), garments.garments, categories.categories, diary.entries, deleted) { outfit, allGarments, allCategories, entries, isDeleted ->
            if (outfit == null || isDeleted) {
                OutfitDetailUiState.Gone
            } else {
                val pieces = PieceFactory(allGarments, allCategories, photos::file)
                OutfitDetailUiState.Loaded(
                    outfit = outfit,
                    pieces = pieces.of(outfit.items),
                    background = pieces.background(outfit.background),
                    garments = outfit.items.mapNotNull { pieces.tile(it.garmentId) },
                    wears = entries.wearsOfOutfit(outfitId),
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), OutfitDetailUiState.Loading)

    fun rate(change: RatingChange<OutfitAspect>) {
        viewModelScope.launch { outfits.rate(outfitId, change) }
    }

    fun delete() {
        deleted.value = true
        viewModelScope.launch { outfits.delete(outfitId) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
