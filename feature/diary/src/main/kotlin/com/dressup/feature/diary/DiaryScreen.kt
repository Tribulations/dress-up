package com.dressup.feature.diary

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dressup.core.data.DressUpData
import com.dressup.core.ui.DressUpColors
import com.dressup.core.ui.OutfitBoard
import com.dressup.core.ui.RatingBar
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** The Diary tab: every logged wear as a list, newest first, by month (ARCHITECTURE.md §7.7). */
@Composable
fun DiaryRoute(
    data: DressUpData,
    profileName: String,
    onOpenEntry: (Long) -> Unit,
    onLogWear: () -> Unit,
    onOpenSettings: () -> Unit,
    bottomBar: @Composable () -> Unit = {},
) {
    val viewModel: DiaryViewModel = viewModel(
        factory = viewModelFactory {
            initializer { DiaryViewModel(data.diary, data.garments, data.categories, data.outfits, data.photos) }
        },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DiaryScreen(state, profileName, onOpenEntry, onLogWear, onOpenSettings, bottomBar)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryScreen(
    state: DiaryUiState,
    profileName: String,
    onOpenEntry: (Long) -> Unit,
    onLogWear: () -> Unit,
    onOpenSettings: () -> Unit,
    bottomBar: @Composable () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.diary_title))
                        Text(profileName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.diary_settings))
                    }
                },
            )
        },
        bottomBar = bottomBar,
        floatingActionButton = {
            if (state is DiaryUiState.Loaded && state.hasGarments) {
                FloatingActionButton(
                    onClick = onLogWear,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.diary_log_wear))
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                state !is DiaryUiState.Loaded -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                !state.hasGarments && state.isEmpty -> Message(stringResource(R.string.diary_no_garments))
                state.isEmpty -> Message(stringResource(R.string.diary_empty))
                else -> DiaryList(state.months, onOpenEntry)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DiaryList(months: List<DiaryMonth>, onOpenEntry: (Long) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(bottom = 88.dp), modifier = Modifier.fillMaxSize()) {
        months.forEach { month ->
            stickyHeader(key = "month-${month.month}") { MonthHeader(month.month) }
            items(month.rows, key = { it.entry.id }) { row ->
                DiaryRowItem(row, onClick = { onOpenEntry(row.entry.id) })
                HorizontalDivider(Modifier.padding(start = 72.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            }
        }
    }
}

@Composable
private fun MonthHeader(month: YearMonth) {
    Text(
        month.format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale.getDefault())).uppercase(Locale.getDefault()),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

/** One wear: the day on the left, a small collage, then what was worn, the occasion and the rating. */
@Composable
private fun DiaryRowItem(row: DiaryRow, onClick: () -> Unit) {
    val entry = row.entry
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Column(Modifier.width(44.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(entry.date.dayOfMonth.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(
                entry.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutfitBoard(row.worn.pieces, Modifier.width(54.dp), background = row.worn.background)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(wornTitle(row.worn), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            entry.occasion?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.worn.rating?.let { RatingBar(it, starSize = 14.dp) }
                if (row.worn.liked) Icon(Icons.Filled.Favorite, contentDescription = null, tint = DressUpColors.Rose, modifier = Modifier.size(14.dp))
                if (entry.note != null) {
                    Icon(
                        Icons.Filled.Create,
                        contentDescription = stringResource(R.string.diary_has_note),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
internal fun Message(text: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}
