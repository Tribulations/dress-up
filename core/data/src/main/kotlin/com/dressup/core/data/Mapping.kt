package com.dressup.core.data

import com.dressup.core.data.db.CategoryEntity
import com.dressup.core.data.db.GarmentWithDetails
import com.dressup.core.data.db.OutfitWithItems
import com.dressup.core.model.Category
import com.dressup.core.model.Garment
import com.dressup.core.model.GarmentAspect
import com.dressup.core.model.GarmentColour
import com.dressup.core.model.Outfit
import com.dressup.core.model.OutfitItem
import com.dressup.core.model.Rating

internal fun GarmentWithDetails.toModel() = Garment(
    id = garment.id,
    name = garment.name,
    description = garment.description,
    categoryIds = categories.sortedBy { it.position }.map { it.categoryId },
    colour = GarmentColour.fromKeyOrNull(garment.colourKey),
    photoFileName = garment.photoFileName,
    cutoutFileName = garment.cutoutFileName,
    rating = Rating.fromHalfStarsOrNull(garment.ratingHalfStars),
    aspectRatings = aspectRatings.mapNotNull { row ->
        val aspect = GarmentAspect.fromKeyOrNull(row.aspectKey) ?: return@mapNotNull null
        val rating = Rating.fromHalfStarsOrNull(row.halfStars) ?: return@mapNotNull null
        aspect to rating
    }.toMap(),
    review = garment.review,
    liked = garment.liked,
    createdAtMillis = garment.createdAtMillis,
)

internal fun OutfitWithItems.toModel() = Outfit(
    id = outfit.id,
    name = outfit.name,
    items = items.sortedBy { it.position }.map { OutfitItem(it.garmentId, it.x, it.y, it.scale) },
    createdAtMillis = outfit.createdAtMillis,
)

internal fun CategoryEntity.toModel() = Category(
    id = id,
    name = name,
    builtIn = builtInKey != null,
    hidden = hidden,
)
