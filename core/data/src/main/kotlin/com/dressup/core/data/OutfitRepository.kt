package com.dressup.core.data

import androidx.room.withTransaction
import com.dressup.core.data.db.DressUpDatabase
import com.dressup.core.data.db.OutfitEntity
import com.dressup.core.data.db.OutfitItemEntity
import com.dressup.core.model.MAX_OUTFIT_NAME_LENGTH
import com.dressup.core.model.Outfit
import com.dressup.core.model.OutfitDraft
import com.dressup.core.model.cleanText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Outfits: garments put together on a collage board (ARCHITECTURE.md §5.2). */
interface OutfitRepository {
    /** Newest first. */
    val outfits: Flow<List<Outfit>>

    fun outfit(id: Long): Flow<Outfit?>

    /** Saves a new outfit or replaces an existing one's name and board, and returns its id. */
    suspend fun save(draft: OutfitDraft): Long

    /** Deletes the outfit only; its garments stay in the wardrobe. */
    suspend fun delete(id: Long)
}

internal class RoomOutfitRepository(
    private val database: DressUpDatabase,
    private val clock: () -> Long = System::currentTimeMillis,
) : OutfitRepository {
    private val dao = database.outfitDao()

    override val outfits: Flow<List<Outfit>> = dao.observeAll().map { list -> list.map { it.toModel() } }

    override fun outfit(id: Long): Flow<Outfit?> = dao.observe(id).map { it?.toModel() }

    override suspend fun save(draft: OutfitDraft): Long = database.withTransaction {
        val name = cleanText(draft.name, MAX_OUTFIT_NAME_LENGTH)
        val id = draft.id?.also { dao.rename(it, name) }
            ?: dao.insert(OutfitEntity(name = name, createdAtMillis = clock()))
        dao.deleteItems(id)
        dao.insertItems(
            draft.items.distinctBy { it.garmentId }.mapIndexed { position, item ->
                OutfitItemEntity(id, item.garmentId, position, item.x, item.y, item.scale)
            },
        )
        id
    }

    override suspend fun delete(id: Long) = dao.delete(id)
}
