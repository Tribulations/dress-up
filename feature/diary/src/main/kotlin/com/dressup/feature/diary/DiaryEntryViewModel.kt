package com.dressup.feature.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dressup.core.data.CategoryRepository
import com.dressup.core.data.DiaryRepository
import com.dressup.core.data.GarmentRepository
import com.dressup.core.data.OutfitRepository
import com.dressup.core.data.photo.PhotoStorage
import com.dressup.core.model.DiaryEntry
import com.dressup.core.ui.GalleryTile
import com.dressup.core.ui.PieceFactory
import com.dressup.core.ui.WhileUiSubscribed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface DiaryEntryUiState {
    data object Loading : DiaryEntryUiState
    data object Gone : DiaryEntryUiState
    data class Loaded(
        val entry: DiaryEntry,
        val worn: WornThing,
        /** The outfit to open, while it still exists. */
        val outfitId: Long?,
        /** Every garment worn, to open one. */
        val garments: List<GalleryTile>,
    ) : DiaryEntryUiState
}

/** One diary entry's page: the date, occasion and note, and what was worn, leading on to the outfit and its garments. */
class DiaryEntryViewModel(
    private val entryId: Long,
    private val diary: DiaryRepository,
    garments: GarmentRepository,
    categories: CategoryRepository,
    outfits: OutfitRepository,
    photos: PhotoStorage,
) : ViewModel() {
    private val deleted = MutableStateFlow(false)

    val uiState: StateFlow<DiaryEntryUiState> =
        combine(diary.entry(entryId), garments.garments, categories.categories, outfits.outfits, deleted) { entry, allGarments, allCategories, allOutfits, isDeleted ->
            if (entry == null || isDeleted) {
                DiaryEntryUiState.Gone
            } else {
                val pieces = PieceFactory(allGarments, allCategories, photos::file)
                DiaryEntryUiState.Loaded(
                    entry = entry,
                    worn = WornThings(allGarments, allCategories, allOutfits, photos::file).of(entry),
                    outfitId = entry.outfitId?.takeIf { id -> allOutfits.any { it.id == id } },
                    garments = entry.garmentIds.mapNotNull(pieces::tile),
                )
            }
        }.stateIn(viewModelScope, WhileUiSubscribed, DiaryEntryUiState.Loading)

    fun delete() {
        deleted.value = true
        viewModelScope.launch { diary.delete(entryId) }
    }
}
