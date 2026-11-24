package com.dressup.feature.garment

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dressup.core.data.CategoryNameResult
import com.dressup.core.data.CategoryRepository
import com.dressup.core.data.GarmentRepository
import com.dressup.core.data.photo.PhotoStorage
import com.dressup.core.model.Category
import com.dressup.core.model.GarmentColour
import com.dressup.core.model.GarmentDraft
import com.dressup.core.model.MAX_DESCRIPTION_LENGTH
import com.dressup.core.model.MAX_GARMENT_NAME_LENGTH
import com.dressup.core.ui.SavedDraft
import com.dressup.core.ui.WhileUiSubscribed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.io.File

/** The editor's inputs, kept as a [SavedDraft], so a camera trip that kills the app doesn't lose them. */
@Serializable
private data class EditorFields(
    val loaded: Boolean,
    val name: String = "",
    val description: String = "",
    /** In the order picked: the first is the main category. */
    val categoryIds: List<Long> = emptyList(),
    /** In the order picked: the first is the main colour. */
    val colours: List<GarmentColour> = emptyList(),
    val photoFileName: String? = null,
    /** The photo the garment had when the editor opened; never deleted here, only replaced on save. */
    val originalPhoto: String? = null,
)

private data class EditorStatus(
    val importingPhoto: Boolean = false,
    val saving: Boolean = false,
    val savedGarmentId: Long? = null,
    val message: EditorMessage? = null,
    val categoryDialog: CategoryDialogState? = null,
)

enum class EditorMessage { PHOTO_FAILED, CAMERA_MISSING, SAVE_FAILED }

data class CategoryDialogState(val error: CategoryNameError? = null)

enum class CategoryNameError { BLANK, DUPLICATE }

data class GarmentEditorUiState(
    val isNew: Boolean,
    val loaded: Boolean,
    /** The categories to pick from: visible ones, plus the garment's own if it has been hidden since. */
    val categories: List<Category>,
    val name: String,
    val description: String,
    /** In the order picked: the first is the main category. */
    val selectedCategoryIds: List<Long>,
    val colours: List<GarmentColour>,
    val photo: File?,
    val importingPhoto: Boolean,
    val saving: Boolean,
    val savedGarmentId: Long?,
    val message: EditorMessage?,
    val categoryDialog: CategoryDialogState?,
) {
    /** A category is the only required field (ARCHITECTURE.md §5). */
    val canSave: Boolean get() = loaded && selectedCategoryIds.isNotEmpty() && !importingPhoto && !saving
}

class GarmentEditorViewModel(
    private val garmentId: Long?,
    private val savedState: SavedStateHandle,
    private val garments: GarmentRepository,
    private val categories: CategoryRepository,
    private val photos: PhotoStorage,
) : ViewModel() {
    private val draft = SavedDraft(savedState, EditorFields.serializer()) { EditorFields(loaded = false) }
    private val status = MutableStateFlow(EditorStatus())

    val uiState: StateFlow<GarmentEditorUiState> =
        combine(draft.flow, status, categories.categories, ::stateOf)
            .stateIn(viewModelScope, WhileUiSubscribed, stateOf(EditorFields(loaded = false), EditorStatus(), emptyList()))

    private fun stateOf(f: EditorFields, s: EditorStatus, allCategories: List<Category>) = GarmentEditorUiState(
        isNew = garmentId == null,
        loaded = f.loaded,
        categories = allCategories.filter { !it.hidden || it.id in f.categoryIds },
        name = f.name,
        description = f.description,
        selectedCategoryIds = f.categoryIds,
        colours = f.colours,
        photo = f.photoFileName?.let(photos::file),
        importingPhoto = s.importingPhoto,
        saving = s.saving,
        savedGarmentId = s.savedGarmentId,
        message = s.message,
        categoryDialog = s.categoryDialog,
    )

    init {
        if (!draft.value.loaded) loadExisting()
    }

    private fun loadExisting() {
        if (garmentId == null) {
            draft.update { it.copy(loaded = true) }
            return
        }
        viewModelScope.launch {
            val garment = garments.garment(garmentId).first()
            draft.update {
                EditorFields(
                    loaded = true,
                    name = garment?.name.orEmpty(),
                    description = garment?.description.orEmpty(),
                    categoryIds = garment?.categoryIds.orEmpty(),
                    colours = garment?.colours.orEmpty(),
                    photoFileName = garment?.photoFileName,
                    originalPhoto = garment?.photoFileName,
                )
            }
        }
    }

    /** Picks or unpicks a category. A garment can have several; the first picked is its main one. */
    fun toggleCategory(id: Long) = draft.update {
        it.copy(categoryIds = if (id in it.categoryIds) it.categoryIds - id else it.categoryIds + id)
    }

    fun setName(name: String) = draft.update { it.copy(name = name.take(MAX_GARMENT_NAME_LENGTH)) }

    fun setDescription(description: String) = draft.update { it.copy(description = description.take(MAX_DESCRIPTION_LENGTH)) }

    /** Tapping a colour adds it, or takes it away again. */
    fun toggleColour(colour: GarmentColour) = draft.update {
        it.copy(colours = if (colour in it.colours) it.colours - colour else it.colours + colour)
    }

    fun clearColours() = draft.update { it.copy(colours = emptyList()) }

    fun newCaptureUri(): Uri = photos.newCaptureUri()

    fun cameraMissing() = status.update { it.copy(message = EditorMessage.CAMERA_MISSING) }

    /** Called with the camera's output or a photo picked from the gallery. */
    fun importPhoto(source: Uri) {
        status.update { it.copy(importingPhoto = true) }
        viewModelScope.launch {
            photos.import(source)
                .onSuccess { name -> replacePhoto(name) }
                .onFailure { status.update { it.copy(message = EditorMessage.PHOTO_FAILED) } }
            status.update { it.copy(importingPhoto = false) }
        }
    }

    fun removePhoto() = replacePhoto(null)

    private fun replacePhoto(newName: String?) {
        val previous = draft.value.photoFileName
        draft.update { it.copy(photoFileName = newName) }
        // A photo taken in this editor and replaced before saving is never used: delete it now.
        if (previous != null && previous != draft.value.originalPhoto && previous != newName) {
            viewModelScope.launch { photos.delete(previous) }
        }
    }

    fun save() {
        val current = draft.value
        if (current.categoryIds.isEmpty()) return
        status.update { it.copy(saving = true) }
        viewModelScope.launch {
            runCatching {
                garments.save(
                    GarmentDraft(
                        id = garmentId,
                        name = current.name,
                        description = current.description,
                        categoryIds = current.categoryIds,
                        colours = current.colours,
                        photoFileName = current.photoFileName,
                    ),
                )
            }.onSuccess { id ->
                status.update { it.copy(saving = false, savedGarmentId = id) }
            }.onFailure {
                status.update { it.copy(saving = false, message = EditorMessage.SAVE_FAILED) }
            }
        }
    }

    fun messageShown() = status.update { it.copy(message = null) }

    fun openCategoryDialog() = status.update { it.copy(categoryDialog = CategoryDialogState()) }

    fun closeCategoryDialog() = status.update { it.copy(categoryDialog = null) }

    fun addCategory(name: String) {
        viewModelScope.launch {
            when (val result = categories.add(name)) {
                is CategoryNameResult.Saved -> {
                    if (result.id !in draft.value.categoryIds) toggleCategory(result.id)
                    closeCategoryDialog()
                }
                CategoryNameResult.Blank -> setDialogError(CategoryNameError.BLANK)
                CategoryNameResult.Duplicate -> setDialogError(CategoryNameError.DUPLICATE)
            }
        }
    }

    private fun setDialogError(error: CategoryNameError) =
        status.update { it.copy(categoryDialog = CategoryDialogState(error)) }
}
