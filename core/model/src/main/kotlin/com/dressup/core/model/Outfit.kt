package com.dressup.core.model

/**
 * Garments put together as one look (ARCHITECTURE.md §5.2), shown as a collage: each [items] entry places one garment's
 * picture on the board. The list order is the drawing order, the last one on top.
 */
data class Outfit(
    val id: Long,
    val name: String?,
    val items: List<OutfitItem>,
    val createdAtMillis: Long,
)

/**
 * Where a garment sits on the outfit board. [x] and [y] are its centre as fractions of the board's width and height
 * (0..1); [scale] is its width as a fraction of the board's width.
 */
data class OutfitItem(
    val garmentId: Long,
    val x: Float,
    val y: Float,
    val scale: Float,
) {
    companion object {
        const val MIN_SCALE = 0.15f
        const val MAX_SCALE = 1.2f
        const val DEFAULT_SCALE = 0.45f
    }
}

/** What the outfit editor saves: a new outfit when [id] is null, otherwise a replacement of an existing one. */
data class OutfitDraft(
    val id: Long?,
    val name: String?,
    val items: List<OutfitItem>,
)

const val MAX_OUTFIT_NAME_LENGTH = 40

/**
 * Where garments added to an outfit start: two columns, three rows, top to bottom, each fully on the 3:4 board, so
 * nothing is cut off or hidden under something else before the user arranges it. A seventh garment starts over at the
 * top.
 */
fun defaultOutfitLayout(garmentIds: List<Long>, existing: Int = 0): List<OutfitItem> = garmentIds.mapIndexed { i, id ->
    val slot = existing + i
    val column = slot % 2
    val row = (slot / 2) % 3
    OutfitItem(garmentId = id, x = 0.28f + 0.44f * column, y = 0.18f + 0.32f * row, scale = OutfitItem.DEFAULT_SCALE)
}
