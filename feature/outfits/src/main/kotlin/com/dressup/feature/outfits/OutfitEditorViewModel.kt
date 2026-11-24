package com.dressup.feature.outfits

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dressup.core.data.CategoryRepository
import com.dressup.core.data.GarmentRepository
import com.dressup.core.data.OutfitRepository
import com.dressup.core.data.photo.PhotoStorage
import com.dressup.core.model.BoardColour
import com.dressup.core.model.Category
import com.dressup.core.model.Garment
import com.dressup.core.model.MAX_OUTFIT_NAME_LENGTH
import com.dressup.core.model.OutfitBackground
import com.dressup.core.model.OutfitDraft
import com.dressup.core.model.OutfitItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.dressup.core.ui.BoardBackground
import com.dressup.core.ui.BoardPiece
import com.dressup.core.ui.GalleryTile
import com.dressup.core.ui.PieceFactory
import com.dressup.core.ui.SavedDraft
import com.dressup.core.ui.WhileUiSubscribed
import kotlinx.serialization.Serializable

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
    /** The outfit's background, as chosen, and as the board draws it. */
    val background: OutfitBackground? = null,
    val boardBackground: BoardBackground? = null,
    val importingBackground: Boolean = false,
    val backgroundFailed: Boolean = false,
) {
    val canSave: Boolean get() = loaded && pieces.isNotEmpty() && !saving
    val selectedPiece: BoardPiece? get() = pieces.firstOrNull { it.item.garmentId == selectedGarmentId }
}

/** The editor's inputs, kept as a [SavedDraft]. */
@Serializable
private data class OutfitFields(
    val loaded: Boolean,
    val name: String = "",
    val items: List<OutfitItem> = emptyList(),
    val galleryOpen: Boolean = false,
    val background: OutfitBackground? = null,
    /** The background image the outfit had when the editor opened: only the repository may delete that one. */
    val savedBackgroundFile: String? = null,
)

private data class OutfitStatus(
    val selectedGarmentId: Long? = null,
    val saving: Boolean = false,
    val done: Boolean = false,
    val savedId: Long? = null,
    val saveFailed: Boolean = false,
    val importingBackground: Boolean = false,
    val backgroundFailed: Boolean = false,
)

/** Makes a new outfit when [outfitId] is null, otherwise edits it. */
class OutfitEditorViewModel(
    private val outfitId: Long?,
    private val savedState: SavedStateHandle,
    private val outfits: OutfitRepository,
    garments: GarmentRepository,
    categories: CategoryRepository,
    private val photos: PhotoStorage,
) : ViewModel() {
    private val draft = SavedDraft(savedState, OutfitFields.serializer()) { OutfitFields(loaded = false) }
    private val status = MutableStateFlow(OutfitStatus())

    val uiState: StateFlow<OutfitEditorUiState> =
        combine(draft.flow, status, garments.garments, categories.categories, ::stateOf)
            .stateIn(viewModelScope, WhileUiSubscribed, stateOf(OutfitFields(loaded = false), OutfitStatus(), emptyList(), emptyList()))

    private fun stateOf(f: OutfitFields, s: OutfitStatus, allGarments: List<Garment>, allCategories: List<Category>): OutfitEditorUiState {
        val pieces = PieceFactory(allGarments, allCategories, photos::file)
        return OutfitEditorUiState(
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
            background = f.background,
            boardBackground = pieces.background(f.background),
            importingBackground = s.importingBackground,
            backgroundFailed = s.backgroundFailed,
        )
    }

    init {
        if (!draft.value.loaded) load()
    }

    private fun load() {
        if (outfitId == null) {
            draft.update { it.copy(loaded = true, galleryOpen = true) } // a new outfit starts by picking garments
            return
        }
        viewModelScope.launch {
            val outfit = outfits.outfit(outfitId).first()
            draft.update {
                OutfitFields(
                    loaded = true,
                    name = outfit?.name.orEmpty(),
                    items = outfit?.items.orEmpty(),
                    background = outfit?.background,
                    savedBackgroundFile = (outfit?.background as? OutfitBackground.Image)?.fileName,
                )
            }
        }
    }

    fun setName(name: String) = draft.update { it.copy(name = name.take(MAX_OUTFIT_NAME_LENGTH)) }

    fun openGallery() = draft.update { it.copy(galleryOpen = true) }

    fun closeGallery() = draft.update { it.copy(galleryOpen = false) }

    /** The garments picked in the gallery become the outfit's. */
    fun pickGarments(garmentIds: List<Long>) {
        draft.update { it.copy(items = it.items.withGarments(garmentIds), galleryOpen = false) }
        if (status.value.selectedGarmentId !in garmentIds) select(null)
    }

    /**
     * Selects a garment on the board or in the layer list (where a locked one can be selected too, to unlock it).
     * Selecting never changes the layer order: that is [moveLayer]'s job.
     */
    fun select(garmentId: Long?) = status.update { it.copy(selectedGarmentId = garmentId) }

    /** A locked garment is never moved, whatever the gesture. */
    fun transform(garmentId: Long, dx: Float, dy: Float, zoom: Float, rotation: Float = 0f) {
        if (isLocked(garmentId)) return
        draft.update { it.copy(items = it.items.transformed(garmentId, dx, dy, zoom, rotation)) }
    }

    /** The rotation slider: turns the garment to [degrees]. */
    fun rotate(garmentId: Long, degrees: Float) {
        if (isLocked(garmentId)) return
        draft.update { it.copy(items = it.items.rotated(garmentId, degrees)) }
    }

    /** Moves the selected garment one layer to the front ([steps] 1) or the back (-1). */
    fun moveLayer(steps: Int) {
        val id = status.value.selectedGarmentId ?: return
        draft.update { it.copy(items = it.items.movedLayer(id, steps)) }
    }

    fun toggleLock() {
        val id = status.value.selectedGarmentId ?: return
        draft.update { it.copy(items = it.items.lockToggled(id)) }
    }

    private fun isLocked(garmentId: Long) = draft.value.items.any { it.garmentId == garmentId && it.locked }

    /** A palette colour, or null for no background. */
    fun setBackgroundColour(colour: BoardColour?) = replaceBackground(colour?.let(OutfitBackground::Colour))

    /** The user's own image from the camera or the gallery, imported (downscaled) like a garment photo. */
    fun importBackground(source: Uri) {
        status.update { it.copy(importingBackground = true, backgroundFailed = false) }
        viewModelScope.launch {
            photos.import(source)
                .onSuccess { fileName ->
                    replaceBackground(OutfitBackground.Image(fileName))
                    status.update { it.copy(importingBackground = false) }
                }
                .onFailure { status.update { it.copy(importingBackground = false, backgroundFailed = true) } }
        }
    }

    fun backgroundFailedShown() = status.update { it.copy(backgroundFailed = false) }

    /** An image picked in this editor and then replaced was never saved, so its file goes at once. */
    private fun replaceBackground(background: OutfitBackground?) {
        val previous = (draft.value.background as? OutfitBackground.Image)?.fileName
        draft.update { it.copy(background = background) }
        if (previous != null && previous != draft.value.savedBackgroundFile && previous != (background as? OutfitBackground.Image)?.fileName) {
            viewModelScope.launch { photos.delete(previous) }
        }
    }

    fun resize(garmentId: Long, scale: Float) {
        if (isLocked(garmentId)) return
        draft.update { it.copy(items = it.items.resized(garmentId, scale)) }
    }

    fun removeSelected() {
        val id = status.value.selectedGarmentId ?: return
        draft.update { f -> f.copy(items = f.items.filterNot { it.garmentId == id }) }
        select(null)
    }

    fun save() {
        val current = draft.value
        if (current.items.isEmpty()) return
        status.update { it.copy(saving = true, saveFailed = false) }
        viewModelScope.launch {
            runCatching { outfits.save(OutfitDraft(outfitId, current.name, current.items, current.background)) }
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
}
