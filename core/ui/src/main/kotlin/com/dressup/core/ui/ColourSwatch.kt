package com.dressup.core.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dressup.core.model.GarmentColour

/** A small round sample of a garment colour. "Multi" is drawn as a rainbow. */
@Composable
fun ColourSwatch(colour: GarmentColour, modifier: Modifier = Modifier, size: Dp = 16.dp) {
    val fill = if (colour == GarmentColour.MULTI) {
        Modifier.background(Brush.sweepGradient(MULTI_COLOURS))
    } else {
        Modifier.background(Color(colour.argb))
    }
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .then(fill)
            .border(1.dp, DressUpColors.Outline, CircleShape),
    )
}

private val MULTI_COLOURS = listOf(
    Color(0xFFC62828), Color(0xFFF2C94C), Color(0xFF4C8C4A), Color(0xFF3E7CC9), Color(0xFF7E57C2), Color(0xFFC62828),
)

@Composable
fun GarmentColour.label(): String = stringResource(labelRes)

private val GarmentColour.labelRes: Int
    @StringRes get() = when (this) {
        GarmentColour.BLACK -> R.string.core_ui_colour_black
        GarmentColour.WHITE -> R.string.core_ui_colour_white
        GarmentColour.GREY -> R.string.core_ui_colour_grey
        GarmentColour.BEIGE -> R.string.core_ui_colour_beige
        GarmentColour.BROWN -> R.string.core_ui_colour_brown
        GarmentColour.RED -> R.string.core_ui_colour_red
        GarmentColour.PINK -> R.string.core_ui_colour_pink
        GarmentColour.ORANGE -> R.string.core_ui_colour_orange
        GarmentColour.YELLOW -> R.string.core_ui_colour_yellow
        GarmentColour.GREEN -> R.string.core_ui_colour_green
        GarmentColour.BLUE -> R.string.core_ui_colour_blue
        GarmentColour.NAVY -> R.string.core_ui_colour_navy
        GarmentColour.PURPLE -> R.string.core_ui_colour_purple
        GarmentColour.MULTI -> R.string.core_ui_colour_multi
    }
