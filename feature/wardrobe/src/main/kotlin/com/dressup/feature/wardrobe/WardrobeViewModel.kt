package com.dressup.feature.wardrobe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dressup.core.data.CategoryRepository
import com.dressup.core.data.GarmentRepository
import com.dressup.core.data.photo.PhotoStorage
import com.dressup.core.model.Category
import com.dressup.core.model.Garment
import com.dressup.core.model.GarmentColour
import com.dressup.core.ui.garmentTitle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.io.File

/** [title]: the garment's name or main category. [picture]: its cut-out or photo. */
data class WardrobeTile(val garment: Garment, val title: String, val picture: File?) {
    val isCutout: Boolean get() = garment.cutoutFileName != null
}

sealed interface WardrobeUiState {
    data object Loading : WardrobeUiState

    data class Loaded(
        val tiles: List<WardrobeTile>,
        val totalCount: Int,
        val filter: WardrobeFilter,
        /** Only categories and colours some garment has, so every chip gives a result. */
        val categoriesInUse: List<Category>,
        val coloursInUse: List<GarmentColour>,
    ) : WardrobeUiState
}

class WardrobeViewModel(
    garments: GarmentRepository,
    categories: CategoryRepository,
    private val photos: PhotoStorage,
) : ViewModel() {
    private val filter = MutableStateFlow(WardrobeFilter())

    val uiState: StateFlow<WardrobeUiState> =
        combine(garments.garments, categories.categories, filter) { allGarments, allCategories, currentFilter ->
            buildState(allGarments, allCategories, currentFilter)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), WardrobeUiState.Loading)

    private fun buildState(garments: List<Garment>, categories: List<Category>, filter: WardrobeFilter): WardrobeUiState {
        val byId = categories.associateBy { it.id }
        val usedCategoryIds = garments.flatMap { it.categoryIds }.toSet()
        val usedColours = garments.mapNotNull { it.colour }.toSet()
        return WardrobeUiState.Loaded(
            tiles = applyFilter(garments, categories, filter).map { garment ->
                WardrobeTile(garment, garmentTitle(garment, byId), garment.pictureFileName?.let(photos::file))
            },
            totalCount = garments.size,
            filter = filter,
            categoriesInUse = categories.filter { it.id in usedCategoryIds },
            coloursInUse = GarmentColour.entries.filter { it in usedColours },
        )
    }

    fun toggleCategory(id: Long) = filter.update { it.copy(categoryIds = it.categoryIds.toggle(id)) }

    fun toggleColour(colour: GarmentColour) = filter.update { it.copy(colours = it.colours.toggle(colour)) }

    fun toggleLikedOnly() = filter.update { it.copy(likedOnly = !it.likedOnly) }

    fun setSort(sort: WardrobeSort) = filter.update { it.copy(sort = sort) }

    fun clearFilters() = filter.update { WardrobeFilter(sort = it.sort) }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

private fun <T> Set<T>.toggle(item: T): Set<T> = if (item in this) this - item else this + item
