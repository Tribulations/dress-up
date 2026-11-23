package com.dressup.feature.garment

import com.dressup.core.data.CutoutResult
import com.dressup.core.data.DiaryRepository
import com.dressup.core.data.GarmentRepository
import com.dressup.core.model.DiaryDraft
import com.dressup.core.model.DiaryEntry
import com.dressup.core.model.Garment
import com.dressup.core.model.GarmentAspect
import com.dressup.core.model.GarmentDraft
import com.dressup.core.model.Rating
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

internal fun testGarment(id: Long, categoryIds: List<Long>, photo: String? = null, cutout: String? = null) = Garment(
    id = id,
    name = null,
    description = null,
    categoryIds = categoryIds,
    colours = emptyList(),
    photoFileName = photo,
    cutoutFileName = cutout,
    rating = null,
    aspectRatings = emptyMap(),
    review = null,
    liked = false,
    createdAtMillis = 0,
)

internal class FakeGarments : GarmentRepository {
    val items = MutableStateFlow<List<Garment>>(emptyList())
    val savedDrafts = mutableListOf<GarmentDraft>()
    var cutoutResult = CutoutResult.DONE
    override val canCutOut = true
    override val garments: Flow<List<Garment>> = items
    override fun garment(id: Long): Flow<Garment?> = items.map { list -> list.firstOrNull { it.id == id } }
    override suspend fun save(draft: GarmentDraft): Long {
        savedDrafts += draft
        return draft.id ?: savedDrafts.size.toLong()
    }
    override suspend fun setRating(id: Long, rating: Rating?) = change(id) { it.copy(rating = rating) }
    override suspend fun setAspectRating(id: Long, aspect: GarmentAspect, rating: Rating?) = change(id) {
        it.copy(aspectRatings = if (rating == null) it.aspectRatings - aspect else it.aspectRatings + (aspect to rating))
    }
    override suspend fun setReview(id: Long, review: String?) = change(id) { it.copy(review = review?.trim()?.ifEmpty { null }) }
    override suspend fun setLiked(id: Long, liked: Boolean) = change(id) { it.copy(liked = liked) }
    override suspend fun cutOut(id: Long): CutoutResult {
        if (cutoutResult == CutoutResult.DONE) change(id) { it.copy(cutoutFileName = "cutout.webp") }
        return cutoutResult
    }
    override suspend fun removeCutout(id: Long) = change(id) { it.copy(cutoutFileName = null) }
    override suspend fun delete(id: Long) = items.update { list -> list.filterNot { it.id == id } }

    private fun change(id: Long, edit: (Garment) -> Garment) =
        items.update { list -> list.map { if (it.id == id) edit(it) else it } }
}

internal class FakeDiary : DiaryRepository {
    val items = MutableStateFlow<List<DiaryEntry>>(emptyList())
    override val entries: Flow<List<DiaryEntry>> = items
    override fun entry(id: Long): Flow<DiaryEntry?> = items.map { list -> list.firstOrNull { it.id == id } }
    override val occasions: Flow<List<String>> = MutableStateFlow(emptyList())
    override suspend fun save(draft: DiaryDraft) = 0L
    override suspend fun delete(id: Long) = Unit
}
