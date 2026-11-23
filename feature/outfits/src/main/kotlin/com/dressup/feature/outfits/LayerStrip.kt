package com.dressup.feature.outfits

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dressup.core.ui.BoardPiece
import com.dressup.core.ui.GarmentPhoto

/**
 * Every garment of the outfit as a layer, the front one first, like the layers of a drawing app. Tapping one selects
 * it on the board, including a locked one (to unlock it).
 */
@Composable
internal fun LayerStrip(pieces: List<BoardPiece>, selectedGarmentId: Long?, onSelect: (Long) -> Unit, modifier: Modifier = Modifier) {
    if (pieces.isEmpty()) return
    Column(modifier) {
        Text(
            stringResource(R.string.outfits_layers),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 6.dp)) {
            items(pieces.asReversed(), key = { it.item.garmentId }) { piece ->
                Layer(piece, selected = piece.item.garmentId == selectedGarmentId, onClick = { onSelect(piece.item.garmentId) })
            }
        }
    }
}

@Composable
private fun Layer(piece: BoardPiece, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    val outline = if (selected) {
        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape)
    } else {
        Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(LAYER_SIZE)
            .clip(shape)
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics { this.selected = selected },
    ) {
        Box(Modifier.size(LAYER_SIZE).then(outline).padding(4.dp)) {
            GarmentPhoto(
                photo = piece.picture,
                placeholder = piece.title,
                contentDescription = null,
                isCutout = piece.isCutout,
                cutoutPadding = 0.dp,
                modifier = Modifier.matchParentSize().clip(RoundedCornerShape(4.dp)),
            )
            if (piece.item.locked) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), modifier = Modifier.align(Alignment.TopEnd)) {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = stringResource(R.string.outfits_locked),
                        modifier = Modifier.padding(3.dp).size(14.dp),
                    )
                }
            }
        }
        Text(
            piece.title,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

private val LAYER_SIZE = 64.dp
