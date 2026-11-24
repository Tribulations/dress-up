package com.dressup.feature.diary

import com.dressup.core.model.Category
import com.dressup.core.model.DiaryEntry
import com.dressup.core.model.Garment
import com.dressup.core.model.Outfit
import com.dressup.core.model.OutfitItem
import com.dressup.core.model.Rating
import com.dressup.core.model.defaultOutfitLayout
import com.dressup.core.ui.BoardBackground
import com.dressup.core.ui.BoardPiece
import com.dressup.core.ui.PieceFactory
import java.io.File
import java.time.YearMonth

/** What a diary entry shows: what was worn, as a name and a small collage. */
data class WornThing(
    /** The outfit's name, or null for an outfit without one (the screen then says "Outfit"). */
    val outfitName: String?,
    val isOutfit: Boolean,
    /** The garments' titles, for an entry of single garments. */
    val garmentTitles: List<String>,
    /** The outfit's board, or the garments laid out like a new outfit. */
    val pieces: List<BoardPiece>,
    /** The outfit's rating, or a single garment's. */
    val rating: Rating?,
    val liked: Boolean,
    /** The outfit's background; single garments have none. */
    val background: BoardBackground? = null,
)

data class DiaryRow(val entry: DiaryEntry, val worn: WornThing)

/** The diary list's months, newest first, each with its entries. */
data class DiaryMonth(val month: YearMonth, val rows: List<DiaryRow>)

/** Puts together what a diary entry shows from the wardrobe and outfits as they are now. */
internal class WornThings(garments: List<Garment>, categories: List<Category>, outfits: List<Outfit>, pictureFile: (String) -> File) {
    private val pieces = PieceFactory(garments, categories, pictureFile)
    private val garmentsById = garments.associateBy { it.id }
    private val outfitsById = outfits.associateBy { it.id }

    fun of(entry: DiaryEntry): WornThing {
        val outfit = entry.outfitId?.let(outfitsById::get)
        if (outfit != null) {
            // The outfit's board as it is now, but only with what was actually worn that day.
            val board = outfit.items.filter { it.garmentId in entry.garmentIds }
            return WornThing(outfit.name, isOutfit = true, emptyList(), pieces.of(board), outfit.ratings.overall, outfit.ratings.liked, pieces.background(outfit.background))
        }
        val worn = entry.garmentIds.mapNotNull(garmentsById::get)
        val single = worn.singleOrNull()
        return WornThing(
            outfitName = null,
            isOutfit = false,
            garmentTitles = worn.map(pieces::title),
            pieces = pieces.of(collageLayout(worn.map { it.id })),
            rating = single?.ratings?.overall,
            liked = single?.ratings?.liked ?: false,
        )
    }

}

/**
 * How single garments (no outfit) fill a small collage: one large in the middle, two above each other, three or four
 * in a 2×2 grid, more like a new outfit's board.
 */
internal fun collageLayout(garmentIds: List<Long>): List<OutfitItem> = when (garmentIds.size) {
    1 -> listOf(OutfitItem(garmentIds[0], 0.5f, 0.5f, 0.9f))
    2 -> garmentIds.mapIndexed { i, id -> OutfitItem(id, 0.5f, 0.27f + 0.46f * i, 0.62f) }
    3, 4 -> garmentIds.mapIndexed { i, id -> OutfitItem(id, 0.26f + 0.48f * (i % 2), 0.3f + 0.4f * (i / 2), 0.48f) }
    else -> defaultOutfitLayout(garmentIds)
}

/** Groups [rows] (newest first) by month. */
internal fun byMonth(rows: List<DiaryRow>): List<DiaryMonth> =
    rows.groupBy { YearMonth.from(it.entry.date) }.map { (month, inMonth) -> DiaryMonth(month, inMonth) }
