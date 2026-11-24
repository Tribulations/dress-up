package com.dressup.core.data

import android.graphics.Bitmap
import android.util.Log
import androidx.room.withTransaction
import com.dressup.core.data.db.DressUpDatabase
import com.dressup.core.data.db.GarmentCategoryEntity
import com.dressup.core.data.db.GarmentEntity
import com.dressup.core.data.photo.BackgroundRemoval
import com.dressup.core.data.photo.BackgroundRemover
import com.dressup.core.data.photo.FilePhotoStorage
import com.dressup.core.data.photo.opaqueBounds
import com.dressup.core.model.Garment
import com.dressup.core.model.GarmentAspect
import com.dressup.core.model.GarmentColour
import com.dressup.core.model.GarmentDraft
import com.dressup.core.model.MAX_DESCRIPTION_LENGTH
import com.dressup.core.model.MAX_GARMENT_NAME_LENGTH
import com.dressup.core.model.RatingChange
import com.dressup.core.model.cleanText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

interface GarmentRepository : RatingStore<GarmentAspect> {
    val garments: Flow<List<Garment>>

    fun garment(id: Long): Flow<Garment?>

    /**
     * Saves a new garment or updates an existing one, and returns its id. [GarmentDraft.categoryIds] must not be empty.
     * A replaced photo file is deleted, and so is the old photo's cut-out.
     */
    suspend fun save(draft: GarmentDraft): Long

    /** Whether this build can remove backgrounds at all ([BackgroundRemover.isAvailable]). */
    val canCutOut: Boolean

    /** Makes a cut-out of the garment's photo (the photo itself is kept, so this can be undone). */
    suspend fun cutOut(id: Long): CutoutResult

    /** Goes back to showing the photo: deletes the cut-out. */
    suspend fun removeCutout(id: Long)

    /** Deletes the garment, its photo and cut-out files; it also disappears from the outfits it was in. */
    suspend fun delete(id: Long)
}

enum class CutoutResult { DONE, NO_PHOTO, NOTHING_FOUND, UNAVAILABLE, FAILED }

internal class RoomGarmentRepository(
    private val database: DressUpDatabase,
    private val photos: FilePhotoStorage,
    private val backgroundRemover: BackgroundRemover,
    private val clock: () -> Long = System::currentTimeMillis,
) : GarmentRepository {
    private val dao = database.garmentDao()
    private val ratings = RoomRatingStore<GarmentAspect>(dao)

    override val garments: Flow<List<Garment>> = dao.observeAll().map { list -> list.map { it.toModel() } }

    override fun garment(id: Long): Flow<Garment?> = dao.observe(id).map { it?.toModel() }

    override suspend fun save(draft: GarmentDraft): Long {
        require(draft.categoryIds.isNotEmpty()) { "A garment needs a category" }
        val name = cleanText(draft.name, MAX_GARMENT_NAME_LENGTH)
        val description = cleanText(draft.description, MAX_DESCRIPTION_LENGTH)
        val existingId = draft.id
        if (existingId == null) {
            return database.withTransaction {
                val id = dao.insert(
                    GarmentEntity(
                        name = name,
                        description = description,
                        colourKeys = GarmentColour.keysOf(draft.colours),
                        photoFileName = draft.photoFileName,
                        ratingHalfStars = null,
                        liked = false,
                        createdAtMillis = clock(),
                    ),
                )
                dao.insertCategories(links(id, draft.categoryIds))
                id
            }
        }
        val previous = dao.get(existingId)
        val photoChanged = previous != null && previous.photoFileName != draft.photoFileName
        database.withTransaction {
            dao.updateDetails(existingId, name, description, GarmentColour.keysOf(draft.colours), draft.photoFileName)
            dao.deleteCategories(existingId)
            dao.insertCategories(links(existingId, draft.categoryIds))
            if (photoChanged) dao.setCutout(existingId, null)
        }
        // Only after the row points at the new photo, so a crash in between never loses the garment's picture.
        if (photoChanged) {
            previous.photoFileName?.let { photos.delete(it) }
            previous.cutoutFileName?.let { photos.delete(it) }
        }
        return existingId
    }

    private fun links(garmentId: Long, categoryIds: List<Long>) =
        categoryIds.distinct().mapIndexed { position, categoryId -> GarmentCategoryEntity(garmentId, categoryId, position) }

    override suspend fun rate(id: Long, change: RatingChange<GarmentAspect>) = ratings.rate(id, change)

    override val canCutOut: Boolean get() = backgroundRemover.isAvailable

    override suspend fun cutOut(id: Long): CutoutResult {
        val garment = dao.get(id) ?: return CutoutResult.NO_PHOTO
        val photo = garment.photoFileName ?: return CutoutResult.NO_PHOTO
        val bitmap = try {
            withContext(Dispatchers.IO) { photos.decode(photo) }
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't read the photo for a cut-out", e)
            return CutoutResult.FAILED
        }
        val result = when (val removal = backgroundRemover.removeBackground(bitmap)) {
            is BackgroundRemoval.Done -> saveCutout(garment, removal.cutout)
            BackgroundRemoval.NothingFound -> CutoutResult.NOTHING_FOUND
            BackgroundRemoval.Unavailable -> CutoutResult.UNAVAILABLE
            is BackgroundRemoval.Failed -> {
                Log.w(TAG, "Background removal failed", removal.cause)
                CutoutResult.FAILED
            }
        }
        bitmap.recycle()
        return result
    }

    private suspend fun saveCutout(garment: GarmentEntity, cutout: Bitmap): CutoutResult {
        val fileName = withContext(Dispatchers.Default) {
            val pixels = IntArray(cutout.width * cutout.height)
            cutout.getPixels(pixels, 0, cutout.width, 0, 0, cutout.width, cutout.height)
            val bounds = opaqueBounds(pixels, cutout.width, cutout.height) ?: return@withContext null
            val cropped = Bitmap.createBitmap(cutout, bounds.left, bounds.top, bounds.width, bounds.height)
            withContext(Dispatchers.IO) { photos.saveCutout(cropped) }.also {
                if (cropped !== cutout) cropped.recycle()
                cutout.recycle()
            }
        } ?: return CutoutResult.NOTHING_FOUND
        dao.setCutout(garment.id, fileName)
        garment.cutoutFileName?.let { photos.delete(it) }
        return CutoutResult.DONE
    }

    override suspend fun removeCutout(id: Long) {
        val cutout = dao.get(id)?.cutoutFileName ?: return
        dao.setCutout(id, null)
        photos.delete(cutout)
    }

    override suspend fun delete(id: Long) {
        val garment = dao.get(id)
        dao.delete(id)
        garment?.photoFileName?.let { photos.delete(it) }
        garment?.cutoutFileName?.let { photos.delete(it) }
    }

    private companion object {
        const val TAG = "GarmentRepository"
    }
}
