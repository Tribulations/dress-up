package com.dressup.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.dressup.core.model.Category
import com.dressup.core.model.Garment
import com.dressup.core.model.OutfitBackground
import com.dressup.core.model.OutfitItem
import com.dressup.core.model.topmostAt
import java.io.File
import kotlin.math.PI
import kotlin.math.abs
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
 * Read-only unless [onTransform] is given. Then a quick tap selects the topmost unlocked garment under the finger, or
 * nothing on an empty spot ([onSelect]). Dragging, pinching and twisting anywhere on the board move the selected
 * garment, even when the fingers are on other garments, so a crowded board never steals the gesture (ARCHITECTURE.md
 * §7.6). With nothing selected, a drag that starts on a garment selects and moves it. Positions are fractions of the
 * board, so the same outfit looks the same at any size. [background] goes behind everything.
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
    val currentPieces by rememberUpdatedState(pieces)
    val currentSelected by rememberUpdatedState(selectedGarmentId)
    val currentSelect by rememberUpdatedState(onSelect)
    val currentTransform by rememberUpdatedState(onTransform)
    val editing = if (onTransform == null) {
        Modifier
    } else {
        Modifier.pointerInput(Unit) {
            boardGestures(
                items = { currentPieces.map { it.item } },
                selected = { currentSelected },
                onSelect = { currentSelect?.invoke(it) },
                onTransform = { id, dx, dy, zoom, rotation -> currentTransform?.invoke(id, dx, dy, zoom, rotation) },
            )
        }
    }
    val fill = when (background) {
        is BoardBackground.Colour -> Modifier.background(Color(background.argb))
        else -> Modifier.background(Brush.verticalGradient(listOf(colours.surfaceVariant, colours.surfaceContainerHighest)))
    }
    BoxWithConstraints(
        modifier
            .aspectRatio(BOARD_ASPECT_RATIO)
            .clip(RoundedCornerShape(8.dp))
            .then(fill)
            .then(editing),
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
                    onClick = onPieceClick.takeIf { onTransform == null },
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
    onClick: ((Long) -> Unit)?,
) {
    val id = piece.item.garmentId
    val sidePx = piece.item.scale * boardWidth
    val left = piece.item.x * boardWidth - sidePx / 2
    val top = piece.item.y * boardHeight - sidePx / 2
    val gestures = if (onClick == null) Modifier else Modifier.clickable(onClickLabel = piece.title) { onClick(id) }
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

/**
 * The editing board's gestures, on the whole board (see [OutfitBoard]). A gesture that stays within the touch slop with
 * one finger is a tap; anything more is a move, pinch or twist of the selected garment.
 */
private suspend fun PointerInputScope.boardGestures(
    items: () -> List<OutfitItem>,
    selected: () -> Long?,
    onSelect: (Long?) -> Unit,
    onTransform: (garmentId: Long, dx: Float, dy: Float, zoom: Float, rotation: Float) -> Unit,
) = awaitEachGesture {
    val down = awaitFirstDown(requireUnconsumed = false)
    val width = size.width.toFloat()
    val height = size.height.toFloat()
    val selectedId = selected()
    // The selected garment takes every gesture; with none, the garment under the first finger does.
    val target = selectedId?.takeIf { id -> items().none { it.garmentId == id && it.locked } }
        ?: if (selectedId == null) items().topmostAt(down.position.x, down.position.y, width, height) else null
    var transforming = false
    var severalFingers = false
    var zoom = 1f
    var rotation = 0f
    var pan = Offset.Zero
    do {
        val event = awaitPointerEvent()
        if (event.changes.any { it.isConsumed }) break
        if (event.changes.count { it.pressed } > 1) severalFingers = true
        val zoomChange = event.calculateZoom()
        val rotationChange = event.calculateRotation()
        val panChange = event.calculatePan()
        if (!transforming) {
            zoom *= zoomChange
            rotation += rotationChange
            pan += panChange
            val centroidSize = event.calculateCentroidSize(useCurrent = false)
            val zoomMotion = abs(1 - zoom) * centroidSize
            val rotationMotion = abs(rotation * PI.toFloat() * centroidSize / 180f)
            transforming = zoomMotion > viewConfiguration.touchSlop ||
                rotationMotion > viewConfiguration.touchSlop ||
                pan.getDistance() > viewConfiguration.touchSlop
            if (transforming && target != null && target != selectedId) onSelect(target)
        }
        if (transforming && target != null) {
            if (zoomChange != 1f || rotationChange != 0f || panChange != Offset.Zero) {
                onTransform(target, panChange.x / width, panChange.y / height, zoomChange, rotationChange)
            }
            event.changes.forEach { if (it.positionChanged()) it.consume() }
        }
    } while (event.changes.any { it.pressed })
    if (!transforming && !severalFingers) {
        onSelect(items().topmostAt(down.position.x, down.position.y, width, height))
    }
}
