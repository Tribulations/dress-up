package com.dressup.feature.outfits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dressup.core.data.CategoryRepository
import com.dressup.core.data.GarmentRepository
import com.dressup.core.data.OutfitRepository
import com.dressup.core.data.photo.PhotoStorage
import com.dressup.core.model.Category
import com.dressup.core.model.Garment
import com.dressup.core.model.Outfit
import com.dressup.core.model.OutfitItem
import com.dressup.core.ui.garmentTitle
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class OutfitCard(val outfit: Outfit, val pieces: List<BoardPiece>)

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
            val pieces = PieceFactory(allGarments, allCategories, photos)
            OutfitsUiState.Loaded(
                outfits = allOutfits.map { OutfitCard(it, pieces.of(it.items)) },
                hasGarments = allGarments.isNotEmpty(),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), OutfitsUiState.Loading)

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

/** Turns board positions into [BoardPiece]s with each garment's picture and title. */
internal class PieceFactory(garments: List<Garment>, categories: List<Category>, private val photos: PhotoStorage) {
    private val garmentsById = garments.associateBy { it.id }
    private val categoriesById = categories.associateBy { it.id }

    fun title(garment: Garment): String = garmentTitle(garment, categoriesById)

    /** Items whose garment no longer exists are left out. */
    fun of(items: List<OutfitItem>): List<BoardPiece> = items.mapNotNull { item ->
        val garment = garmentsById[item.garmentId] ?: return@mapNotNull null
        BoardPiece(item, title(garment), garment.pictureFileName?.let(photos::file), garment.cutoutFileName != null)
    }
}
