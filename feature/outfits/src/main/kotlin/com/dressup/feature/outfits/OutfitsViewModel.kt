package com.dressup.feature.outfits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dressup.core.data.CategoryRepository
import com.dressup.core.data.GarmentRepository
import com.dressup.core.data.OutfitRepository
import com.dressup.core.data.photo.PhotoStorage
import com.dressup.core.model.Outfit
import com.dressup.core.ui.BoardBackground
import com.dressup.core.ui.BoardPiece
import com.dressup.core.ui.PieceFactory
import com.dressup.core.ui.WhileUiSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class OutfitCard(val outfit: Outfit, val pieces: List<BoardPiece>, val background: BoardBackground?)

sealed interface OutfitsUiState {
    data object Loading : OutfitsUiState
    data class Loaded(val outfits: List<OutfitCard>, val hasGarments: Boolean) : OutfitsUiState
}

class OutfitsViewModel(
    outfits: OutfitRepository,
    garments: GarmentRepository,
    categories: CategoryRepository,
    private val photos: PhotoStorage,
) : ViewModel() {
    val uiState: StateFlow<OutfitsUiState> =
        combine(outfits.outfits, garments.garments, categories.categories) { allOutfits, allGarments, allCategories ->
            val pieces = PieceFactory(allGarments, allCategories, photos::file)
            OutfitsUiState.Loaded(
                outfits = allOutfits.map { OutfitCard(it, pieces.of(it.items), pieces.background(it.background)) },
                hasGarments = allGarments.isNotEmpty(),
            )
        }.stateIn(viewModelScope, WhileUiSubscribed, OutfitsUiState.Loading)
}
