package com.dressup.feature.diary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dressup.core.data.CategoryRepository
import com.dressup.core.data.DiaryRepository
import com.dressup.core.data.GarmentRepository
import com.dressup.core.data.OutfitRepository
import com.dressup.core.data.photo.PhotoStorage
import com.dressup.core.model.Category
import com.dressup.core.model.DiaryDraft
import com.dressup.core.model.DiaryEntry
import com.dressup.core.model.Garment
import com.dressup.core.model.Outfit
import com.dressup.core.model.MAX_NOTE_LENGTH
import com.dressup.core.model.MAX_OCCASION_LENGTH
import com.dressup.core.ui.BoardBackground
import com.dressup.core.ui.BoardPiece
import com.dressup.core.ui.GalleryTile
import com.dressup.core.ui.PieceFactory
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
import java.time.LocalDate

/** Which picker covers the editor, if any. */
enum class WornPicker { NONE, OUTFIT, GARMENTS }

/** An outfit to pick in the editor's outfit picker. */
data class OutfitChoice(val id: Long, val name: String?, val pieces: List<BoardPiece>, val background: BoardBackground?)

data class DiaryEditorUiState(
    val isNew: Boolean,
    val loaded: Boolean,
    val date: LocalDate,
    val today: LocalDate,
    val outfitId: Long?,
    val garmentIds: List<Long>,
    val occasion: String,
    val note: String,
    /** What has been picked, as the entry will show it; null while nothing is. */
    val worn: WornThing?,
    /** Earlier occasions that match what is typed. */
    val suggestions: List<String>,
    val picker: WornPicker,
    val outfits: List<OutfitChoice>,
    val gallery: List<GalleryTile>,
    val saving: Boolean,
    val done: Boolean,
    val saveFailed: Boolean,
) {
    val hasSomethingWorn: Boolean get() = outfitId != null || garmentIds.isNotEmpty()
    val canSave: Boolean get() = loaded && hasSomethingWorn && !saving
}

/** The editor's inputs, kept as a [SavedDraft]. */
@Serializable
private data class EditorFields(
    val loaded: Boolean = false,
    val epochDay: Long,
    val outfitId: Long? = null,
    val garmentIds: List<Long> = emptyList(),
    val occasion: String = "",
    val note: String = "",
    val picker: WornPicker = WornPicker.NONE,
)

private data class EditorStatus(val saving: Boolean = false, val done: Boolean = false, val saveFailed: Boolean = false)

/**
 * Logs a wear, or edits entry [entryId]. A new entry can start from an outfit ([presetOutfitId], from its page) or a
 * garment ([presetGarmentId]), dated today. The inputs live in [SavedStateHandle], so nothing is lost if Android stops
 * the app in the background.
 */
class DiaryEditorViewModel(
    private val entryId: Long?,
    private val presetOutfitId: Long?,
    private val presetGarmentId: Long?,
    private val savedState: SavedStateHandle,
    private val diary: DiaryRepository,
    garments: GarmentRepository,
    categories: CategoryRepository,
    private val outfits: OutfitRepository,
    photos: PhotoStorage,
    private val today: () -> LocalDate = LocalDate::now,
) : ViewModel() {
    private val draft = SavedDraft(savedState, EditorFields.serializer()) { EditorFields(epochDay = today().toEpochDay()) }
    private val status = MutableStateFlow(EditorStatus())

    private val wardrobe = combine(garments.garments, categories.categories, outfits.outfits, diary.occasions) { g, c, o, occasions ->
        Wardrobe(g, c, o, occasions, photos::file)
    }

    val uiState: StateFlow<DiaryEditorUiState> = combine(draft.flow, status, wardrobe, ::stateOf)
        .stateIn(viewModelScope, WhileUiSubscribed, stateOf(EditorFields(epochDay = today().toEpochDay()), EditorStatus(), Wardrobe.EMPTY))

    private fun stateOf(f: EditorFields, s: EditorStatus, w: Wardrobe): DiaryEditorUiState {
        val worn = if (f.outfitId != null || f.garmentIds.isNotEmpty()) {
            w.worn.of(DiaryEntry(0, LocalDate.ofEpochDay(f.epochDay), f.outfitId, f.garmentIds, null, null, 0))
        } else {
            null
        }
        return DiaryEditorUiState(
            isNew = entryId == null,
            loaded = f.loaded,
            date = LocalDate.ofEpochDay(f.epochDay),
            today = today(),
            outfitId = f.outfitId,
            garmentIds = f.garmentIds,
            occasion = f.occasion,
            note = f.note,
            worn = worn,
            suggestions = suggestions(w.occasions, f.occasion),
            picker = f.picker,
            outfits = w.outfits.map { OutfitChoice(it.id, it.name, w.pieces.of(it.items), w.pieces.background(it.background)) },
            gallery = w.garmentIds.mapNotNull(w.pieces::tile),
            saving = s.saving,
            done = s.done,
            saveFailed = s.saveFailed,
        )
    }

    init {
        if (!draft.value.loaded) load()
    }

    private fun load() {
        viewModelScope.launch {
            val loaded = when {
                entryId != null -> diary.entry(entryId).first()?.let { entry ->
                    EditorFields(
                        loaded = true,
                        epochDay = entry.date.toEpochDay(),
                        outfitId = entry.outfitId,
                        garmentIds = entry.garmentIds,
                        occasion = entry.occasion.orEmpty(),
                        note = entry.note.orEmpty(),
                    )
                }
                presetOutfitId != null -> outfits.outfit(presetOutfitId).first()?.let { outfit ->
                    draft.value.copy(loaded = true, outfitId = outfit.id, garmentIds = outfit.items.map { it.garmentId })
                }
                presetGarmentId != null -> draft.value.copy(loaded = true, garmentIds = listOf(presetGarmentId))
                else -> null
            }
            draft.update { loaded ?: it.copy(loaded = true) }
        }
    }

    fun setDate(date: LocalDate) = draft.update { it.copy(epochDay = minOf(date, today()).toEpochDay()) }

    fun setOccasion(occasion: String) = draft.update { it.copy(occasion = occasion.take(MAX_OCCASION_LENGTH)) }

    fun setNote(note: String) = draft.update { it.copy(note = note.take(MAX_NOTE_LENGTH)) }

    fun openPicker(picker: WornPicker) = draft.update { it.copy(picker = picker) }

    fun closePicker() = draft.update { it.copy(picker = WornPicker.NONE) }

    /** Wearing an outfit means wearing its garments: they are copied in, so the entry keeps them if the outfit changes. */
    fun chooseOutfit(outfitId: Long) {
        viewModelScope.launch {
            val outfit = outfits.outfit(outfitId).first() ?: return@launch
            draft.update { it.copy(outfitId = outfit.id, garmentIds = outfit.items.map { item -> item.garmentId }, picker = WornPicker.NONE) }
        }
    }

    /** Single garments instead of an outfit. */
    fun pickGarments(garmentIds: List<Long>) =
        draft.update { it.copy(outfitId = null, garmentIds = garmentIds, picker = WornPicker.NONE) }

    fun save() {
        val f = draft.value
        if (!f.loaded || (f.outfitId == null && f.garmentIds.isEmpty())) return
        status.update { it.copy(saving = true, saveFailed = false) }
        viewModelScope.launch {
            runCatching {
                diary.save(DiaryDraft(entryId, LocalDate.ofEpochDay(f.epochDay), f.outfitId, f.garmentIds, f.occasion, f.note))
            }
                .onSuccess { status.update { it.copy(saving = false, done = true) } }
                .onFailure { status.update { it.copy(saving = false, saveFailed = true) } }
        }
    }

    fun saveFailedShown() = status.update { it.copy(saveFailed = false) }

    /** Everything the editor shows besides its own inputs. */
    private class Wardrobe(
        garments: List<Garment>,
        categories: List<Category>,
        val outfits: List<Outfit>,
        val occasions: List<String>,
        pictureFile: (String) -> File,
    ) {
        val worn = WornThings(garments, categories, outfits, pictureFile)
        val pieces = PieceFactory(garments, categories, pictureFile)
        val garmentIds = garments.map { it.id }

        companion object {
            val EMPTY = Wardrobe(emptyList(), emptyList(), emptyList(), emptyList()) { File(it) }
        }
    }
}

/**
 * Earlier occasions to suggest: the most recent few while nothing is typed, else those containing the text (not the
 * text itself).
 */
internal fun suggestions(occasions: List<String>, typed: String): List<String> {
    val text = typed.trim()
    if (text.isEmpty()) return occasions.take(RECENT_SUGGESTIONS)
    return occasions.filter { it.contains(text, ignoreCase = true) && !it.equals(text, ignoreCase = true) }.take(MATCHING_SUGGESTIONS)
}

private const val RECENT_SUGGESTIONS = 5
private const val MATCHING_SUGGESTIONS = 8
