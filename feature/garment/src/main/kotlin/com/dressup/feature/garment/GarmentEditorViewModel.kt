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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

/** The editor's inputs. Kept in [SavedStateHandle], so a camera trip that kills the app doesn't lose them. */
private data class EditorFields(
    val loaded: Boolean,
    val name: String = "",
    val description: String = "",
    /** In the order picked: the first is the main category. */
    val categoryIds: List<Long> = emptyList(),
    val colour: GarmentColour? = null,
    val photoFileName: String? = null,
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
    val colour: GarmentColour?,
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
    private val fields = MutableStateFlow(restoreFields())
    private val status = MutableStateFlow(EditorStatus())

    /** The photo the garment had when the editor opened; never deleted here, only replaced on save. */
    private var originalPhoto: String? = savedState[KEY_ORIGINAL_PHOTO]

    val uiState: StateFlow<GarmentEditorUiState> =
        combine(fields, status, categories.categories) { f, s, allCategories ->
            GarmentEditorUiState(
                isNew = garmentId == null,
                loaded = f.loaded,
                categories = allCategories.filter { !it.hidden || it.id in f.categoryIds },
                name = f.name,
                description = f.description,
                selectedCategoryIds = f.categoryIds,
                colour = f.colour,
                photo = f.photoFileName?.let(photos::file),
                importingPhoto = s.importingPhoto,
                saving = s.saving,
                savedGarmentId = s.savedGarmentId,
                message = s.message,
                categoryDialog = s.categoryDialog,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), initialState())

    init {
        if (!fields.value.loaded) loadExisting()
    }

    private fun loadExisting() {
        if (garmentId == null) {
            setFields { it.copy(loaded = true) }
            return
        }
        viewModelScope.launch {
            val garment = garments.garment(garmentId).first()
            originalPhoto = garment?.photoFileName
            savedState[KEY_ORIGINAL_PHOTO] = originalPhoto
            setFields {
                EditorFields(
                    loaded = true,
                    name = garment?.name.orEmpty(),
                    description = garment?.description.orEmpty(),
                    categoryIds = garment?.categoryIds.orEmpty(),
                    colour = garment?.colour,
                    photoFileName = garment?.photoFileName,
                )
            }
        }
    }

    /** Picks or unpicks a category. A garment can have several; the first picked is its main one. */
    fun toggleCategory(id: Long) = setFields {
        it.copy(categoryIds = if (id in it.categoryIds) it.categoryIds - id else it.categoryIds + id)
    }

    fun setName(name: String) = setFields { it.copy(name = name.take(MAX_GARMENT_NAME_LENGTH)) }

    fun setDescription(description: String) = setFields { it.copy(description = description.take(MAX_DESCRIPTION_LENGTH)) }

    fun selectColour(colour: GarmentColour?) = setFields { it.copy(colour = colour) }

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
        val previous = fields.value.photoFileName
        setFields { it.copy(photoFileName = newName) }
        // A photo taken in this editor and replaced before saving is never used: delete it now.
        if (previous != null && previous != originalPhoto && previous != newName) {
            viewModelScope.launch { photos.delete(previous) }
        }
    }

    fun save() {
        val current = fields.value
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
                        colour = current.colour,
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
                    if (result.id !in fields.value.categoryIds) toggleCategory(result.id)
                    closeCategoryDialog()
                }
                CategoryNameResult.Blank -> setDialogError(CategoryNameError.BLANK)
                CategoryNameResult.Duplicate -> setDialogError(CategoryNameError.DUPLICATE)
            }
        }
    }

    private fun setDialogError(error: CategoryNameError) =
        status.update { it.copy(categoryDialog = CategoryDialogState(error)) }

    private fun setFields(change: (EditorFields) -> EditorFields) {
        val updated = change(fields.value)
        fields.value = updated
        savedState[KEY_LOADED] = updated.loaded
        savedState[KEY_NAME] = updated.name
        savedState[KEY_DESCRIPTION] = updated.description
        savedState[KEY_CATEGORIES] = updated.categoryIds.toLongArray()
        savedState[KEY_COLOUR] = updated.colour?.key
        savedState[KEY_PHOTO] = updated.photoFileName
    }

    private fun restoreFields() = EditorFields(
        loaded = savedState[KEY_LOADED] ?: false,
        name = savedState[KEY_NAME] ?: "",
        description = savedState[KEY_DESCRIPTION] ?: "",
        categoryIds = savedState.get<LongArray>(KEY_CATEGORIES)?.toList().orEmpty(),
        colour = GarmentColour.fromKeyOrNull(savedState[KEY_COLOUR]),
        photoFileName = savedState[KEY_PHOTO],
    )

    private fun initialState() = GarmentEditorUiState(
        isNew = garmentId == null,
        loaded = false,
        categories = emptyList(),
        name = "",
        description = "",
        selectedCategoryIds = emptyList(),
        colour = null,
        photo = null,
        importingPhoto = false,
        saving = false,
        savedGarmentId = null,
        message = null,
        categoryDialog = null,
    )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val KEY_LOADED = "loaded"
        const val KEY_NAME = "name"
        const val KEY_DESCRIPTION = "description"
        const val KEY_CATEGORIES = "categoryIds"
        const val KEY_COLOUR = "colour"
        const val KEY_PHOTO = "photo"
        const val KEY_ORIGINAL_PHOTO = "originalPhoto"
    }
}
