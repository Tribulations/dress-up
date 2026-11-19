package com.dressup.feature.outfits

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.dressup.core.model.OutfitItem
import com.dressup.core.ui.GarmentPhoto
import java.io.File
import kotlin.math.roundToInt

/** One garment as the board draws it: where it sits, and its cut-out or photo. */
data class BoardPiece(val item: OutfitItem, val title: String, val picture: File?, val isCutout: Boolean)

/** The board's shape: a 3:4 portrait, like a full outfit laid out on the floor. */
const val BOARD_ASPECT_RATIO = 3f / 4f

/**
 * An outfit as a collage: each garment's picture placed on a 3:4 board, the last piece on top.
 *
 * Read-only unless [onTransform] is given: then a piece can be dragged and pinched, and tapping one selects it
 * ([onSelect]). Positions are fractions of the board, so the same outfit looks the same at any size.
 */
@Composable
fun OutfitBoard(
    pieces: List<BoardPiece>,
    modifier: Modifier = Modifier,
    selectedGarmentId: Long? = null,
    onSelect: ((Long?) -> Unit)? = null,
    onTransform: ((garmentId: Long, dx: Float, dy: Float, zoom: Float) -> Unit)? = null,
) {
    val colours = MaterialTheme.colorScheme
    val tapToDeselect = if (onSelect == null) Modifier else Modifier.pointerInput(Unit) { detectTapGestures { onSelect(null) } }
    BoxWithConstraints(
        modifier
            .aspectRatio(BOARD_ASPECT_RATIO)
            .clip(RoundedCornerShape(8.dp))
            .background(Brush.verticalGradient(listOf(colours.surfaceVariant, colours.surfaceContainerHighest)))
            .then(tapToDeselect),
    ) {
        val boardWidth = constraints.maxWidth.toFloat()
        val boardHeight = constraints.maxHeight.toFloat()
        pieces.forEach { piece ->
            key(piece.item.garmentId) {
                Piece(
                    piece = piece,
                    boardWidth = boardWidth,
                    boardHeight = boardHeight,
                    selected = piece.item.garmentId == selectedGarmentId,
                    onSelect = onSelect,
                    onTransform = onTransform,
                )
            }
        }
    }
}

@Composable
private fun Piece(
    piece: BoardPiece,
    boardWidth: Float,
    boardHeight: Float,
    selected: Boolean,
    onSelect: ((Long?) -> Unit)?,
    onTransform: ((Long, Float, Float, Float) -> Unit)?,
) {
    val id = piece.item.garmentId
    val sidePx = piece.item.scale * boardWidth
    val left = piece.item.x * boardWidth - sidePx / 2
    val top = piece.item.y * boardHeight - sidePx / 2
    val currentTransform by rememberUpdatedState(onTransform)
    val currentSelect by rememberUpdatedState(onSelect)
    val gestures = if (onTransform == null) {
        Modifier
    } else {
        Modifier
            .pointerInput(id) {
                detectTransformGestures { _, pan, zoom, _ ->
                    currentSelect?.invoke(id)
                    currentTransform?.invoke(id, pan.x / boardWidth, pan.y / boardHeight, zoom)
                }
            }
            .pointerInput(id) { detectTapGestures { currentSelect?.invoke(id) } }
    }
    val outline = if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp)) else Modifier
    val density = LocalDensity.current
    Box(
        Modifier
            .offset { IntOffset(left.roundToInt(), top.roundToInt()) }
            .size(with(density) { sidePx.toDp() })
            .then(outline)
            .then(gestures),
    ) {
        GarmentPhoto(
            photo = piece.picture,
            placeholder = piece.title,
            contentDescription = piece.title,
            isCutout = piece.isCutout,
            cutoutPadding = 2.dp,
            cutoutBackdrop = false, // a cut-out floats on the board; a plain photo stays a picture with corners
            modifier = Modifier.matchParentSize().clip(RoundedCornerShape(6.dp)),
        )
    }
}
