package com.dressup.core.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.dressup.core.model.Category
import com.dressup.core.model.Garment
import com.dressup.core.model.GarmentAspect
import com.dressup.core.model.OutfitAspect

/**
 * How a garment is named on screen: its own name, or else its main category (ARCHITECTURE.md §8). The categories go
 * underneath as [garmentSubtitle].
 */
fun garmentTitle(garment: Garment, categories: Map<Long, Category>): String =
    garment.name ?: garment.categoryIds.firstOrNull()?.let { categories[it]?.name }.orEmpty()

/** "Sweater · Sportswear": all the garment's categories, the main one first. */
fun garmentSubtitle(garment: Garment, categories: Map<Long, Category>): String =
    garment.categoryIds.mapNotNull { categories[it]?.name }.joinToString(" · ")

@Composable
fun GarmentAspect.label(): String = stringResource(labelRes)

@get:StringRes
private val GarmentAspect.labelRes: Int
    get() = when (this) {
        GarmentAspect.FIT -> R.string.core_ui_aspect_fit
        GarmentAspect.COMFORT -> R.string.core_ui_aspect_comfort
        GarmentAspect.QUALITY -> R.string.core_ui_aspect_quality
        GarmentAspect.STYLE -> R.string.core_ui_aspect_style
        GarmentAspect.EASY_CARE -> R.string.core_ui_aspect_easy_care
    }

@Composable
fun OutfitAspect.label(): String = stringResource(
    when (this) {
        OutfitAspect.COMFORT -> R.string.core_ui_outfit_aspect_comfort
        OutfitAspect.STYLE -> R.string.core_ui_outfit_aspect_style
        OutfitAspect.FIT -> R.string.core_ui_outfit_aspect_fit
        OutfitAspect.OCCASION -> R.string.core_ui_outfit_aspect_occasion
    },
)
