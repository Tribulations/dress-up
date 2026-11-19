package com.dressup.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.dressup.core.model.Category
import com.dressup.core.model.Garment
import com.dressup.core.model.OutfitBackground
import com.dressup.core.model.OutfitItem
import java.io.File
import kotlin.math.roundToInt

/** One garment as the board draws it: where it sits, and its cut-out or photo. */
data class BoardPiece(val item: OutfitItem, val title: String, val picture: File?, val isCutout: Boolean)

/** What the board draws behind the garments: a colour, or an image (cropped to fill). Null: the app's own soft backdrop. */
sealed interface BoardBackground {
    data class Colour(val argb: Long) : BoardBackground
    data class Image(val file: File) : BoardBackground
}

/** Turns board positions into [BoardPiece]s with each garment's picture ([pictureFile]) and title. */
class PieceFactory(garments: List<Garment>, categories: List<Category>, private val pictureFile: (String) -> File) {
    private val garmentsById = garments.associateBy { it.id }
    private val categoriesById = categories.associateBy { it.id }

    fun title(garment: Garment): String = garmentTitle(garment, categoriesById)

    /** Items whose garment no longer exists are left out. */
    fun of(items: List<OutfitItem>): List<BoardPiece> = items.mapNotNull { item ->
        val garment = garmentsById[item.garmentId] ?: return@mapNotNull null
        BoardPiece(item, title(garment), garment.pictureFileName?.let(pictureFile), garment.cutoutFileName != null)
    }

    fun tile(garment: Garment): GalleryTile =
        GalleryTile(garment.id, title(garment), garment.pictureFileName?.let(pictureFile), garment.cutoutFileName != null)

    fun tile(garmentId: Long): GalleryTile? = garmentsById[garmentId]?.let(::tile)

    fun background(background: OutfitBackground?): BoardBackground? = when (background) {
        null -> null
        is OutfitBackground.Colour -> BoardBackground.Colour(background.colour.argb)
        is OutfitBackground.Image -> BoardBackground.Image(pictureFile(background.fileName))
    }
}

/** The board's shape: a 3:4 portrait, like a full outfit laid out on the floor. */
const val BOARD_ASPECT_RATIO = 3f / 4f

/**
 * An outfit as a collage: each garment's picture placed on a 3:4 board, the last piece on top.
 *
 * Read-only unless [onTransform] is given: then a piece can be dragged, pinched and twisted with two fingers, and
 * tapping one selects it ([onSelect]). Positions are fractions of the board, so the same outfit looks the same at any
 * size. [background] goes behind everything.
 */
@Composable
fun OutfitBoard(
    pieces: List<BoardPiece>,
    modifier: Modifier = Modifier,
    selectedGarmentId: Long? = null,
    onSelect: ((Long?) -> Unit)? = null,
    onTransform: ((garmentId: Long, dx: Float, dy: Float, zoom: Float, rotation: Float) -> Unit)? = null,
    /** On a read-only board: tapping a piece, e.g. to open that garment. */
    onPieceClick: ((Long) -> Unit)? = null,
    background: BoardBackground? = null,
) {
    val colours = MaterialTheme.colorScheme
    val tapToDeselect = if (onSelect == null) Modifier else Modifier.pointerInput(Unit) { detectTapGestures { onSelect(null) } }
    val fill = when (background) {
        is BoardBackground.Colour -> Modifier.background(Color(background.argb))
        else -> Modifier.background(Brush.verticalGradient(listOf(colours.surfaceVariant, colours.surfaceContainerHighest)))
    }
    BoxWithConstraints(
        modifier
            .aspectRatio(BOARD_ASPECT_RATIO)
            .clip(RoundedCornerShape(8.dp))
            .then(fill)
            .then(tapToDeselect),
    ) {
        if (background is BoardBackground.Image) {
            AsyncImage(model = background.file, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
        }
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
                    onClick = onPieceClick,
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
    onTransform: ((Long, Float, Float, Float, Float) -> Unit)?,
    onClick: ((Long) -> Unit)?,
) {
    val id = piece.item.garmentId
    val sidePx = piece.item.scale * boardWidth
    val left = piece.item.x * boardWidth - sidePx / 2
    val top = piece.item.y * boardHeight - sidePx / 2
    val currentTransform by rememberUpdatedState(onTransform)
    val currentSelect by rememberUpdatedState(onSelect)
    val gestures = if (onTransform == null) {
        if (onClick == null) Modifier else Modifier.clickable(onClickLabel = piece.title) { onClick(id) }
    } else {
        Modifier
            .pointerInput(id) {
                detectTransformGestures { _, pan, zoom, rotation ->
                    currentSelect?.invoke(id)
                    currentTransform?.invoke(id, pan.x / boardWidth, pan.y / boardHeight, zoom, rotation)
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
            .graphicsLayer { rotationZ = piece.item.rotation }
            .then(outline)
            .then(gestures),
    ) {
        GarmentPhoto(
            photo = piece.picture,
            placeholder = piece.title,
            contentDescription = piece.title,
            isCutout = piece.isCutout,
            cutoutPadding = 0.dp,
            cutoutBackdrop = false, // a cut-out floats on the board; a plain photo stays a picture with corners
            modifier = Modifier.matchParentSize().clip(RoundedCornerShape(6.dp)),
        )
    }
}
