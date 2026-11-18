package com.dressup.core.data

import com.dressup.core.data.db.GarmentDao
import com.dressup.core.data.db.GarmentEntity
import com.dressup.core.data.photo.PhotoStorage
import com.dressup.core.model.Garment
import com.dressup.core.model.GarmentDraft
import com.dressup.core.model.Rating
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface GarmentRepository {
    val garments: Flow<List<Garment>>

    fun garment(id: Long): Flow<Garment?>

    /** Saves a new garment or updates an existing one, and returns its id. A replaced photo file is deleted. */
    suspend fun save(draft: GarmentDraft): Long

    suspend fun setRating(id: Long, rating: Rating?)

    suspend fun setLiked(id: Long, liked: Boolean)

    /** Deletes the garment and its photo file. */
    suspend fun delete(id: Long)
}

internal class RoomGarmentRepository(
    private val dao: GarmentDao,
    private val photos: PhotoStorage,
    private val clock: () -> Long = System::currentTimeMillis,
) : GarmentRepository {
    override val garments: Flow<List<Garment>> = dao.observeAll().map { list -> list.map { it.toModel() } }

    override fun garment(id: Long): Flow<Garment?> = dao.observe(id).map { it?.toModel() }

    override suspend fun save(draft: GarmentDraft): Long {
        val existingId = draft.id ?: return insert(draft)
        val previousPhoto = dao.get(existingId)?.photoFileName
        dao.updateDetails(existingId, draft.categoryId, draft.colour?.key, draft.photoFileName)
        // Only after the row points at the new photo, so a crash in between never loses the garment's picture.
        if (previousPhoto != null && previousPhoto != draft.photoFileName) photos.delete(previousPhoto)
        return existingId
    }

    private suspend fun insert(draft: GarmentDraft): Long = dao.insert(
        GarmentEntity(
            categoryId = draft.categoryId,
            colourKey = draft.colour?.key,
            photoFileName = draft.photoFileName,
            ratingHalfStars = null,
            liked = false,
            createdAtMillis = clock(),
        ),
    )

    override suspend fun setRating(id: Long, rating: Rating?) = dao.setRating(id, rating?.halfStars)

    override suspend fun setLiked(id: Long, liked: Boolean) = dao.setLiked(id, liked)

    override suspend fun delete(id: Long) {
        val photo = dao.get(id)?.photoFileName
        dao.delete(id)
        if (photo != null) photos.delete(photo)
    }
}
