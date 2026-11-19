package com.dressup.feature.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dressup.core.data.CategoryRepository
import com.dressup.core.data.DiaryRepository
import com.dressup.core.data.GarmentRepository
import com.dressup.core.data.OutfitRepository
import com.dressup.core.data.photo.PhotoStorage
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

sealed interface DiaryUiState {
    data object Loading : DiaryUiState
    data class Loaded(val months: List<DiaryMonth>, val hasGarments: Boolean) : DiaryUiState {
        val isEmpty: Boolean get() = months.isEmpty()
    }
}

/** The Diary tab: every logged wear, newest first, by month. */
class DiaryViewModel(
    diary: DiaryRepository,
    garments: GarmentRepository,
    categories: CategoryRepository,
    outfits: OutfitRepository,
    photos: PhotoStorage,
) : ViewModel() {
    val uiState: StateFlow<DiaryUiState> =
        combine(diary.entries, garments.garments, categories.categories, outfits.outfits) { entries, allGarments, allCategories, allOutfits ->
            val worn = WornThings(allGarments, allCategories, allOutfits, photos::file)
            DiaryUiState.Loaded(byMonth(entries.map { DiaryRow(it, worn.of(it)) }), hasGarments = allGarments.isNotEmpty())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), DiaryUiState.Loading)

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
