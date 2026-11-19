package com.dressup.feature.diary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dressup.core.data.CategoryRepository
import com.dressup.core.data.DiaryRepository
import com.dressup.core.data.GarmentRepository
import com.dressup.core.data.OutfitRepository
import com.dressup.core.data.photo.PhotoStorage
import com.dressup.core.model.DiaryDraft
import com.dressup.core.model.DiaryEntry
import com.dressup.core.model.MAX_NOTE_LENGTH
import com.dressup.core.model.MAX_OCCASION_LENGTH
import com.dressup.core.ui.BoardBackground
import com.dressup.core.ui.BoardPiece
import com.dressup.core.ui.GalleryTile
import com.dressup.core.ui.PieceFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
    private val fields = MutableStateFlow(restoreFields())
    private val status = MutableStateFlow(EditorStatus())

    private val wardrobe = combine(garments.garments, categories.categories, outfits.outfits, diary.occasions) { g, c, o, occasions ->
        Wardrobe(WornThings(g, c, o, photos::file), PieceFactory(g, c, photos::file), g.map { it.id }, o, occasions)
    }

    val uiState: StateFlow<DiaryEditorUiState> = combine(fields, status, wardrobe) { f, s, w ->
        val worn = if (f.outfitId != null || f.garmentIds.isNotEmpty()) {
            w.worn.of(DiaryEntry(0, LocalDate.ofEpochDay(f.epochDay), f.outfitId, f.garmentIds, null, null, 0))
        } else {
            null
        }
        DiaryEditorUiState(
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
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), initialState())

    init {
        if (!fields.value.loaded) load()
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
                    fields.value.copy(loaded = true, outfitId = outfit.id, garmentIds = outfit.items.map { it.garmentId })
                }
                presetGarmentId != null -> fields.value.copy(loaded = true, garmentIds = listOf(presetGarmentId))
                else -> null
            }
            setFields { loaded ?: it.copy(loaded = true) }
        }
    }

    fun setDate(date: LocalDate) = setFields { it.copy(epochDay = minOf(date, today()).toEpochDay()) }

    fun setOccasion(occasion: String) = setFields { it.copy(occasion = occasion.take(MAX_OCCASION_LENGTH)) }

    fun setNote(note: String) = setFields { it.copy(note = note.take(MAX_NOTE_LENGTH)) }

    fun openPicker(picker: WornPicker) = setFields { it.copy(picker = picker) }

    fun closePicker() = setFields { it.copy(picker = WornPicker.NONE) }

    /** Wearing an outfit means wearing its garments: they are copied in, so the entry keeps them if the outfit changes. */
    fun chooseOutfit(outfitId: Long) {
        viewModelScope.launch {
            val outfit = outfits.outfit(outfitId).first() ?: return@launch
            setFields { it.copy(outfitId = outfit.id, garmentIds = outfit.items.map { item -> item.garmentId }, picker = WornPicker.NONE) }
        }
    }

    /** Single garments instead of an outfit. */
    fun pickGarments(garmentIds: List<Long>) =
        setFields { it.copy(outfitId = null, garmentIds = garmentIds, picker = WornPicker.NONE) }

    fun save() {
        val f = fields.value
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

    private fun setFields(change: (EditorFields) -> EditorFields) {
        val updated = change(fields.value)
        fields.value = updated
        savedState[KEY_LOADED] = updated.loaded
        savedState[KEY_DAY] = updated.epochDay
        savedState[KEY_OUTFIT] = updated.outfitId ?: NO_ID
        savedState[KEY_GARMENTS] = updated.garmentIds.toLongArray()
        savedState[KEY_OCCASION] = updated.occasion
        savedState[KEY_NOTE] = updated.note
        savedState[KEY_PICKER] = updated.picker.name
    }

    private fun restoreFields() = EditorFields(
        loaded = savedState[KEY_LOADED] ?: false,
        epochDay = savedState[KEY_DAY] ?: today().toEpochDay(),
        outfitId = savedState.get<Long>(KEY_OUTFIT)?.takeIf { it != NO_ID },
        garmentIds = savedState.get<LongArray>(KEY_GARMENTS)?.toList().orEmpty(),
        occasion = savedState[KEY_OCCASION] ?: "",
        note = savedState[KEY_NOTE] ?: "",
        picker = savedState.get<String>(KEY_PICKER)?.let { name -> WornPicker.entries.firstOrNull { it.name == name } } ?: WornPicker.NONE,
    )

    private fun initialState() = DiaryEditorUiState(
        isNew = entryId == null,
        loaded = false,
        date = today(),
        today = today(),
        outfitId = null,
        garmentIds = emptyList(),
        occasion = "",
        note = "",
        worn = null,
        suggestions = emptyList(),
        picker = WornPicker.NONE,
        outfits = emptyList(),
        gallery = emptyList(),
        saving = false,
        done = false,
        saveFailed = false,
    )

    private class Wardrobe(
        val worn: WornThings,
        val pieces: PieceFactory,
        val garmentIds: List<Long>,
        val outfits: List<com.dressup.core.model.Outfit>,
        val occasions: List<String>,
    )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val NO_ID = -1L
        const val KEY_LOADED = "loaded"
        const val KEY_DAY = "epochDay"
        const val KEY_OUTFIT = "outfitId"
        const val KEY_GARMENTS = "garmentIds"
        const val KEY_OCCASION = "occasion"
        const val KEY_NOTE = "note"
        const val KEY_PICKER = "picker"
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
