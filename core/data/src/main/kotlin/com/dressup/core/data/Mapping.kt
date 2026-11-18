package com.dressup.core.data

import com.dressup.core.data.db.CategoryEntity
import com.dressup.core.data.db.GarmentEntity
import com.dressup.core.model.Category
import com.dressup.core.model.Garment
import com.dressup.core.model.GarmentColour
import com.dressup.core.model.Rating

internal fun GarmentEntity.toModel() = Garment(
    id = id,
    categoryId = categoryId,
    colour = GarmentColour.fromKeyOrNull(colourKey),
    photoFileName = photoFileName,
    rating = Rating.fromHalfStarsOrNull(ratingHalfStars),
    liked = liked,
    createdAtMillis = createdAtMillis,
)

internal fun CategoryEntity.toModel() = Category(
    id = id,
    name = name,
    builtIn = builtInKey != null,
    hidden = hidden,
)
