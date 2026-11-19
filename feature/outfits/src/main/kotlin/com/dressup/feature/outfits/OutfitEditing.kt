package com.dressup.feature.outfits

import com.dressup.core.model.OutfitItem
import com.dressup.core.model.defaultOutfitLayout

/** Moves garment [garmentId] by a fraction of the board ([dx], [dy]) and resizes it by [zoom], kept on the board. */
internal fun List<OutfitItem>.transformed(garmentId: Long, dx: Float, dy: Float, zoom: Float): List<OutfitItem> = map { item ->
    if (item.garmentId != garmentId) {
        item
    } else {
        item.copy(
            x = (item.x + dx).coerceIn(0f, 1f),
            y = (item.y + dy).coerceIn(0f, 1f),
            scale = (item.scale * zoom).coerceIn(OutfitItem.MIN_SCALE, OutfitItem.MAX_SCALE),
        )
    }
}

internal fun List<OutfitItem>.resized(garmentId: Long, scale: Float): List<OutfitItem> = map { item ->
    if (item.garmentId == garmentId) item.copy(scale = scale.coerceIn(OutfitItem.MIN_SCALE, OutfitItem.MAX_SCALE)) else item
}

/** Draws [garmentId] last, i.e. on top of the others. */
internal fun List<OutfitItem>.broughtToFront(garmentId: Long): List<OutfitItem> {
    val item = firstOrNull { it.garmentId == garmentId } ?: return this
    return this - item + item
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
