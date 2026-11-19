package com.dressup.core.data

import androidx.room.withTransaction
import com.dressup.core.data.db.DiaryEntryEntity
import com.dressup.core.data.db.DiaryEntryGarmentEntity
import com.dressup.core.data.db.DressUpDatabase
import com.dressup.core.model.DiaryDraft
import com.dressup.core.model.DiaryEntry
import com.dressup.core.model.MAX_NOTE_LENGTH
import com.dressup.core.model.MAX_OCCASION_LENGTH
import com.dressup.core.model.cleanText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** The wear diary (ARCHITECTURE.md §5.3): what was worn on which date. */
interface DiaryRepository {
    /** Newest first ([com.dressup.core.model.DIARY_ORDER]). */
    val entries: Flow<List<DiaryEntry>>

    fun entry(id: Long): Flow<DiaryEntry?>

    /** Occasions used before, the most recent first, to suggest while typing a new one. */
    val occasions: Flow<List<String>>

    /**
     * Saves a new entry or updates one, and returns its id. It needs an outfit or at least one garment; the draft's
     * [DiaryDraft.garmentIds] are stored as what was worn.
     */
    suspend fun save(draft: DiaryDraft): Long

    suspend fun delete(id: Long)
}

internal class RoomDiaryRepository(
    private val database: DressUpDatabase,
    private val clock: () -> Long = System::currentTimeMillis,
) : DiaryRepository {
    private val dao = database.diaryDao()

    override val entries: Flow<List<DiaryEntry>> = dao.observeAll().map { list -> list.map { it.toModel() } }

    override fun entry(id: Long): Flow<DiaryEntry?> = dao.observe(id).map { it?.toModel() }

    override val occasions: Flow<List<String>> = dao.observeOccasions(MAX_SUGGESTIONS)

    override suspend fun save(draft: DiaryDraft): Long {
        require(draft.outfitId != null || draft.garmentIds.isNotEmpty()) { "A diary entry needs something that was worn" }
        val occasion = cleanText(draft.occasion, MAX_OCCASION_LENGTH)
        val note = cleanText(draft.note, MAX_NOTE_LENGTH)
        val day = draft.date.toEpochDay()
        return database.withTransaction {
            val id = draft.id?.also { dao.update(it, day, draft.outfitId, occasion, note) }
                ?: dao.insert(DiaryEntryEntity(dateEpochDay = day, outfitId = draft.outfitId, occasion = occasion, note = note, createdAtMillis = clock()))
            dao.deleteGarments(id)
            dao.insertGarments(draft.garmentIds.distinct().mapIndexed { position, garmentId -> DiaryEntryGarmentEntity(id, garmentId, position) })
            id
        }
    }

    override suspend fun delete(id: Long) = dao.delete(id)

    private companion object {
        const val MAX_SUGGESTIONS = 30
    }
}
