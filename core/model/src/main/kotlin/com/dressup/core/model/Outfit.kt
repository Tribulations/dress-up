package com.dressup.core.model

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Garments put together as one look (ARCHITECTURE.md §5.2), shown as a collage: each [items] entry places one garment's
 * picture on the board. The list order is the drawing order, the last one on top.
 */
data class Outfit(
    val id: Long,
    val name: String?,
    val items: List<OutfitItem>,
    val ratings: Ratings<OutfitAspect> = Ratings(),
    val background: OutfitBackground? = null,
    val createdAtMillis: Long,
)

/** What an outfit's board shows behind the garments: one of the palette's colours, or the user's own image. */
sealed interface OutfitBackground {
    data class Colour(val colour: BoardColour) : OutfitBackground

    /** A picture in the profile's photo folder, imported like a garment photo. */
    data class Image(val fileName: String) : OutfitBackground
}

/**
 * The colours an outfit board can have (our own palette, GUIDELINES.md §4.1). [key] is stored, so it must never
 * change once released; [argb] is only for drawing.
 */
enum class BoardColour(val key: String, val argb: Long) {
    WHITE("white", 0xFFF7F5F0),
    CREAM("cream", 0xFFEFE6D2),
    SAND("sand", 0xFFD9CFC2),
    BLUSH("blush", 0xFFE8CFCB),
    SAGE("sage", 0xFFB7C4B1),
    SKY("sky", 0xFFBFD3E6),
    MUSTARD("mustard", 0xFFE3B65A),
    TERRACOTTA("terracotta", 0xFFB8684C),
    SLATE("slate", 0xFF545663),
    PLUM("plum", 0xFF4A3F52),
    CHARCOAL("charcoal", 0xFF2A2A2E),
    BLACK("black", 0xFF111111),
    ;

    companion object {
        fun fromKeyOrNull(key: String?): BoardColour? = entries.firstOrNull { it.key == key }
    }
}

/**
 * Where a garment sits on the outfit board. [x] and [y] are its centre as fractions of the board's width and height
 * (0..1); [scale] is its width as a fraction of the board's width; [rotation] turns it clockwise, in degrees
 * (-180..180).
 */
data class OutfitItem(
    val garmentId: Long,
    val x: Float,
    val y: Float,
    val scale: Float,
    val rotation: Float = 0f,
    /** Locked garments stay where they are: touching them on the board neither selects nor moves them. */
    val locked: Boolean = false,
) {
    companion object {
        const val MIN_SCALE = 0.15f
        const val MAX_SCALE = 1.2f
        const val DEFAULT_SCALE = 0.45f
    }
}

/** [degrees] as an angle of -180..180, the same turn. */
fun normalizedRotation(degrees: Float): Float {
    if (!degrees.isFinite()) return 0f
    val turned = ((degrees % 360f) + 360f) % 360f
    return if (turned > 180f) turned - 360f else turned
}

/** What the outfit editor saves: a new outfit when [id] is null, otherwise a replacement of an existing one. */
data class OutfitDraft(
    val id: Long?,
    val name: String?,
    val items: List<OutfitItem>,
    val background: OutfitBackground? = null,
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

/**
 * The garment a touch at ([px], [py]) pixels lands on, on a board of [boardWidth] × [boardHeight] pixels: the topmost
 * (last drawn) one whose square, turned with it, contains the point. Locked garments are skipped. Null: nothing there.
 */
fun List<OutfitItem>.topmostAt(px: Float, py: Float, boardWidth: Float, boardHeight: Float): Long? = lastOrNull { item ->
    if (item.locked) return@lastOrNull false
    val half = item.scale * boardWidth / 2
    val dx = px - item.x * boardWidth
    val dy = py - item.y * boardHeight
    // Turn the point back by the garment's rotation, so the test is against an upright square.
    val radians = Math.toRadians(-item.rotation.toDouble())
    val localX = dx * cos(radians) - dy * sin(radians)
    val localY = dx * sin(radians) + dy * cos(radians)
    abs(localX) <= half && abs(localY) <= half
}?.garmentId
