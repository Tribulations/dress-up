package com.dressup.feature.garment

import com.dressup.core.data.CutoutResult
import com.dressup.core.data.DiaryRepository
import com.dressup.core.data.GarmentRepository
import com.dressup.core.model.DiaryDraft
import com.dressup.core.model.DiaryEntry
import com.dressup.core.model.Garment
import com.dressup.core.model.GarmentAspect
import com.dressup.core.model.GarmentDraft
import com.dressup.core.model.RatingChange
import com.dressup.core.model.Ratings
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
    ratings = Ratings(),
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
    override suspend fun rate(id: Long, change: RatingChange<GarmentAspect>) = change(id) { garment ->
        val r = garment.ratings
        garment.copy(
            ratings = when (change) {
                is RatingChange.Overall -> r.copy(overall = change.rating)
                is RatingChange.OfAspect -> r.copy(aspects = change.rating?.let { r.aspects + (change.aspect to it) } ?: (r.aspects - change.aspect))
                is RatingChange.Review -> r.copy(review = change.text.trim().ifEmpty { null })
                is RatingChange.Liked -> r.copy(liked = change.liked)
            },
        )
    }
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
