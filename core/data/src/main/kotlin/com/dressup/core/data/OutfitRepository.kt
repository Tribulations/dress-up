package com.dressup.core.data

import androidx.room.withTransaction
import com.dressup.core.data.db.DressUpDatabase
import com.dressup.core.data.db.OutfitEntity
import com.dressup.core.data.db.OutfitItemEntity
import com.dressup.core.model.MAX_OUTFIT_NAME_LENGTH
import com.dressup.core.model.Outfit
import com.dressup.core.data.photo.PhotoStorage
import com.dressup.core.model.OutfitAspect
import com.dressup.core.model.OutfitBackground
import com.dressup.core.model.OutfitDraft
import com.dressup.core.model.RatingChange
import com.dressup.core.model.cleanText
import com.dressup.core.model.normalizedRotation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Outfits: garments put together on a collage board (ARCHITECTURE.md §5.2). */
interface OutfitRepository : RatingStore<OutfitAspect> {
    /** Newest first. */
    val outfits: Flow<List<Outfit>>

    fun outfit(id: Long): Flow<Outfit?>

    /**
     * Saves a new outfit or replaces an existing one's name, board and background, and returns its id. A replaced
     * background image file is deleted.
     */
    suspend fun save(draft: OutfitDraft): Long

    /**
     * Deletes the outfit (and its background image) only; its garments stay in the wardrobe, and its diary entries keep
     * the garments worn.
     */
    suspend fun delete(id: Long)
}

internal class RoomOutfitRepository(
    private val database: DressUpDatabase,
    private val photos: PhotoStorage,
    private val clock: () -> Long = System::currentTimeMillis,
) : OutfitRepository {
    private val dao = database.outfitDao()
    private val ratings = RoomRatingStore<OutfitAspect>(dao)

    override val outfits: Flow<List<Outfit>> = dao.observeAll().map { list -> list.map { it.toModel() } }

    override fun outfit(id: Long): Flow<Outfit?> = dao.observe(id).map { it?.toModel() }

    override suspend fun save(draft: OutfitDraft): Long {
        val name = cleanText(draft.name, MAX_OUTFIT_NAME_LENGTH)
        val colourKey = (draft.background as? OutfitBackground.Colour)?.colour?.key
        val imageFile = (draft.background as? OutfitBackground.Image)?.fileName
        val previousImage = draft.id?.let { dao.get(it) }?.backgroundFileName
        val id = database.withTransaction {
            val id = draft.id?.also { dao.updateDetails(it, name, colourKey, imageFile) }
                ?: dao.insert(OutfitEntity(name = name, createdAtMillis = clock(), backgroundColourKey = colourKey, backgroundFileName = imageFile))
            dao.deleteItems(id)
            dao.insertItems(
                draft.items.distinctBy { it.garmentId }.mapIndexed { position, item ->
                    OutfitItemEntity(id, item.garmentId, position, item.x, item.y, item.scale, normalizedRotation(item.rotation), item.locked)
                },
            )
            id
        }
        // Only once the outfit points at its new background, so a crash in between never loses the picture in use.
        if (previousImage != null && previousImage != imageFile) photos.delete(previousImage)
        return id
    }

    override suspend fun rate(id: Long, change: RatingChange<OutfitAspect>) = ratings.rate(id, change)

    override suspend fun delete(id: Long) {
        val image = dao.get(id)?.backgroundFileName
        dao.delete(id)
        image?.let { photos.delete(it) }
    }
}
