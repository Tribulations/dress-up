package com.dressup.feature.diary

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/** What was worn, in a few words: the outfit's name, or the garments ("Denim jacket + 2 more"). */
@Composable
internal fun wornTitle(worn: WornThing): String = when {
    worn.isOutfit -> worn.outfitName ?: stringResource(R.string.diary_outfit)
    worn.garmentTitles.isEmpty() -> stringResource(R.string.diary_nothing_left)
    worn.garmentTitles.size == 1 -> worn.garmentTitles.single()
    else -> stringResource(R.string.diary_garments_more, worn.garmentTitles.first(), worn.garmentTitles.size - 1)
}
