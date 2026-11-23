package com.dressup.feature.outfits

import com.dressup.core.model.OutfitItem
import com.dressup.core.model.defaultOutfitLayout
import com.dressup.core.model.normalizedRotation

/**
 * Moves garment [garmentId] by a fraction of the board ([dx], [dy]), resizes it by [zoom] and turns it by [rotation]
 * degrees, kept on the board.
 */
internal fun List<OutfitItem>.transformed(garmentId: Long, dx: Float, dy: Float, zoom: Float, rotation: Float = 0f): List<OutfitItem> = map { item ->
    if (item.garmentId != garmentId) {
        item
    } else {
        item.copy(
            x = (item.x + dx).coerceIn(0f, 1f),
            y = (item.y + dy).coerceIn(0f, 1f),
            scale = (item.scale * zoom).coerceIn(OutfitItem.MIN_SCALE, OutfitItem.MAX_SCALE),
            rotation = normalizedRotation(item.rotation + rotation),
        )
    }
}

internal fun List<OutfitItem>.resized(garmentId: Long, scale: Float): List<OutfitItem> = map { item ->
    if (item.garmentId == garmentId) item.copy(scale = scale.coerceIn(OutfitItem.MIN_SCALE, OutfitItem.MAX_SCALE)) else item
}

internal fun List<OutfitItem>.rotated(garmentId: Long, degrees: Float): List<OutfitItem> = map { item ->
    if (item.garmentId == garmentId) item.copy(rotation = normalizedRotation(degrees)) else item
}

/**
 * Moves [garmentId] one layer towards the front ([steps] 1) or the back (-1). The list is the drawing order, so the
 * last item is the front one. At the front or the back already, nothing changes.
 */
internal fun List<OutfitItem>.movedLayer(garmentId: Long, steps: Int): List<OutfitItem> {
    val from = indexOfFirst { it.garmentId == garmentId }
    if (from < 0) return this
    val to = (from + steps).coerceIn(0, lastIndex)
    if (to == from) return this
    return toMutableList().apply { add(to, removeAt(from)) }
}

internal fun List<OutfitItem>.lockToggled(garmentId: Long): List<OutfitItem> = map { item ->
    if (item.garmentId == garmentId) item.copy(locked = !item.locked) else item
}

/**
 * The board after picking [garmentIds] in the gallery: garments still picked keep their place, unpicked ones leave,
 * and new ones are laid out where they don't cover the others.
 */
internal fun List<OutfitItem>.withGarments(garmentIds: Collection<Long>): List<OutfitItem> {
    val kept = filter { it.garmentId in garmentIds }
    val added = garmentIds.filter { id -> kept.none { it.garmentId == id } }
    return kept + defaultOutfitLayout(added, existing = kept.size)
}
