package com.dressup.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dressup.core.model.DiaryEntry
import com.dressup.core.model.summary
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.Instant
import java.time.ZoneId

/** "24 Dec 2025" in the phone's language. */
fun formatDate(date: LocalDate): String = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

/** The same for a moment in time ([System.currentTimeMillis]), on the phone's own calendar. */
fun formatDate(millis: Long): String = formatDate(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate())

/** "Worn 7 times · last 24 Dec 2025", or "Not worn yet". */
@Composable
fun wornText(wears: List<DiaryEntry>): String {
    val summary = wears.summary()
    val last = summary.lastWorn ?: return stringResource(R.string.core_ui_never_worn)
    val times = pluralStringResource(R.plurals.core_ui_worn_times, summary.count, summary.count)
    return stringResource(R.string.core_ui_worn_last, times, formatDate(last))
}

/**
 * The wear block of a garment or outfit page: how often and when last it was worn, a "Log wear" button and the latest
 * diary entries ([wears], newest first), each opening its entry. Long histories are folded.
 */
@Composable
fun WearHistory(wears: List<DiaryEntry>, onLogWear: () -> Unit, onOpenEntry: (Long) -> Unit) {
    var showAll by rememberSaveable { mutableStateOf(false) }
    SectionLabel(stringResource(R.string.core_ui_wear_history))
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(wornText(wears), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        FilledTonalButton(onClick = onLogWear) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.core_ui_log_wear))
        }
    }
    val shown = if (showAll) wears else wears.take(FOLDED_WEARS)
    Column {
        shown.forEach { entry -> WearRow(entry, onClick = { onOpenEntry(entry.id) }) }
    }
    if (!showAll && wears.size > FOLDED_WEARS) {
        TextButton(onClick = { showAll = true }) { Text(stringResource(R.string.core_ui_show_all_wears, wears.size)) }
    }
}

@Composable
private fun WearRow(entry: DiaryEntry, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
    ) {
        Text(formatDate(entry.date), style = MaterialTheme.typography.bodyLarge)
        val detail = entry.occasion ?: entry.note
        if (detail != null) {
            Text(
                detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private const val FOLDED_WEARS = 5
