package com.dressup.feature.outfits

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dressup.core.data.CategoryRepository
import com.dressup.core.data.GarmentRepository
import com.dressup.core.data.OutfitRepository
import com.dressup.core.data.photo.PhotoStorage
import com.dressup.core.model.MAX_OUTFIT_NAME_LENGTH
import com.dressup.core.model.OutfitDraft
import com.dressup.core.model.OutfitItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.dressup.core.ui.BoardPiece
import com.dressup.core.ui.GalleryTile
import com.dressup.core.ui.PieceFactory

data class OutfitEditorUiState(
    val isNew: Boolean,
    val loaded: Boolean,
    val name: String,
    val pieces: List<BoardPiece>,
    val selectedGarmentId: Long?,
    /** The gallery of every garment, open to pick what goes in the outfit. */
    val galleryOpen: Boolean,
    val gallery: List<GalleryTile>,
    val saving: Boolean,
    val done: Boolean,
    /** The saved outfit's id once [done] after a save; null after a delete. */
    val savedId: Long? = null,
    val saveFailed: Boolean,
) {
    val canSave: Boolean get() = loaded && pieces.isNotEmpty() && !saving
    val selectedPiece: BoardPiece? get() = pieces.firstOrNull { it.item.garmentId == selectedGarmentId }
}

/** The editor's inputs. Kept in [SavedStateHandle], so nothing is lost if Android stops the app in the background. */
private data class OutfitFields(
    val loaded: Boolean,
    val name: String = "",
    val items: List<OutfitItem> = emptyList(),
    val galleryOpen: Boolean = false,
)

private data class OutfitStatus(
    val selectedGarmentId: Long? = null,
    val saving: Boolean = false,
    val done: Boolean = false,
    val savedId: Long? = null,
    val saveFailed: Boolean = false,
)

/** Makes a new outfit when [outfitId] is null, otherwise edits it. */
class OutfitEditorViewModel(
    private val outfitId: Long?,
    private val savedState: SavedStateHandle,
    private val outfits: OutfitRepository,
    garments: GarmentRepository,
    categories: CategoryRepository,
    photos: PhotoStorage,
) : ViewModel() {
    private val fields = MutableStateFlow(restoreFields())
    private val status = MutableStateFlow(OutfitStatus())

    val uiState: StateFlow<OutfitEditorUiState> =
        combine(fields, status, garments.garments, categories.categories) { f, s, allGarments, allCategories ->
            val pieces = PieceFactory(allGarments, allCategories, photos::file)
            OutfitEditorUiState(
                isNew = outfitId == null,
                loaded = f.loaded,
                name = f.name,
                pieces = pieces.of(f.items),
                selectedGarmentId = s.selectedGarmentId,
                galleryOpen = f.galleryOpen,
                gallery = allGarments.map(pieces::tile),
                saving = s.saving,
                done = s.done,
                savedId = s.savedId,
                saveFailed = s.saveFailed,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), initialState())

    init {
        if (!fields.value.loaded) load()
    }

    private fun load() {
        if (outfitId == null) {
            setFields { it.copy(loaded = true, galleryOpen = true) } // a new outfit starts by picking garments
            return
        }
        viewModelScope.launch {
            val outfit = outfits.outfit(outfitId).first()
            setFields { OutfitFields(loaded = true, name = outfit?.name.orEmpty(), items = outfit?.items.orEmpty()) }
        }
    }

    fun setName(name: String) = setFields { it.copy(name = name.take(MAX_OUTFIT_NAME_LENGTH)) }

    fun openGallery() = setFields { it.copy(galleryOpen = true) }

    fun closeGallery() = setFields { it.copy(galleryOpen = false) }

    /** The garments picked in the gallery become the outfit's. */
    fun pickGarments(garmentIds: List<Long>) {
        setFields { it.copy(items = it.items.withGarments(garmentIds), galleryOpen = false) }
        if (status.value.selectedGarmentId !in garmentIds) select(null)
    }

    /** Selecting a garment also brings it to the front, so it is the one being moved. */
    fun select(garmentId: Long?) {
        if (garmentId != null && garmentId != status.value.selectedGarmentId) {
            setFields { it.copy(items = it.items.broughtToFront(garmentId)) }
        }
        status.update { it.copy(selectedGarmentId = garmentId) }
    }

    fun transform(garmentId: Long, dx: Float, dy: Float, zoom: Float) =
        setFields { it.copy(items = it.items.transformed(garmentId, dx, dy, zoom)) }

    fun resize(garmentId: Long, scale: Float) = setFields { it.copy(items = it.items.resized(garmentId, scale)) }

    fun removeSelected() {
        val id = status.value.selectedGarmentId ?: return
        setFields { f -> f.copy(items = f.items.filterNot { it.garmentId == id }) }
        select(null)
    }

    fun save() {
        val current = fields.value
        if (current.items.isEmpty()) return
        status.update { it.copy(saving = true, saveFailed = false) }
        viewModelScope.launch {
            runCatching { outfits.save(OutfitDraft(outfitId, current.name, current.items)) }
                .onSuccess { id -> status.update { it.copy(saving = false, done = true, savedId = id) } }
                .onFailure { status.update { it.copy(saving = false, saveFailed = true) } }
        }
    }

    fun saveFailedShown() = status.update { it.copy(saveFailed = false) }

    fun delete() {
        val id = outfitId ?: return
        viewModelScope.launch {
            outfits.delete(id)
            status.update { it.copy(done = true) }
        }
    }

    private fun setFields(change: (OutfitFields) -> OutfitFields) {
        val updated = change(fields.value)
        fields.value = updated
        savedState[KEY_LOADED] = updated.loaded
        savedState[KEY_NAME] = updated.name
        savedState[KEY_GALLERY] = updated.galleryOpen
        savedState[KEY_IDS] = updated.items.map { it.garmentId }.toLongArray()
        savedState[KEY_XS] = updated.items.map { it.x }.toFloatArray()
        savedState[KEY_YS] = updated.items.map { it.y }.toFloatArray()
        savedState[KEY_SCALES] = updated.items.map { it.scale }.toFloatArray()
    }

    private fun restoreFields(): OutfitFields {
        val ids = savedState.get<LongArray>(KEY_IDS) ?: LongArray(0)
        val xs = savedState.get<FloatArray>(KEY_XS) ?: FloatArray(0)
        val ys = savedState.get<FloatArray>(KEY_YS) ?: FloatArray(0)
        val scales = savedState.get<FloatArray>(KEY_SCALES) ?: FloatArray(0)
        val count = minOf(ids.size, xs.size, ys.size, scales.size)
        return OutfitFields(
            loaded = savedState[KEY_LOADED] ?: false,
            name = savedState[KEY_NAME] ?: "",
            items = (0 until count).map { OutfitItem(ids[it], xs[it], ys[it], scales[it]) },
            galleryOpen = savedState[KEY_GALLERY] ?: false,
        )
    }

    private fun initialState() = OutfitEditorUiState(
        isNew = outfitId == null,
        loaded = false,
        name = "",
        pieces = emptyList(),
        selectedGarmentId = null,
        galleryOpen = false,
        gallery = emptyList(),
        saving = false,
        done = false,
        saveFailed = false,
    )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val KEY_LOADED = "loaded"
        const val KEY_NAME = "name"
        const val KEY_GALLERY = "galleryOpen"
        const val KEY_IDS = "garmentIds"
        const val KEY_XS = "xs"
        const val KEY_YS = "ys"
        const val KEY_SCALES = "scales"
    }
}
