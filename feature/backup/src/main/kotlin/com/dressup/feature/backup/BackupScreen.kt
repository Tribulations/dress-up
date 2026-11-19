package com.dressup.feature.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dressup.core.data.backup.ProfileBackup
import com.dressup.core.ui.displayName

/**
 * Export of the active profile ([profileId], shown as [profileName]) and import of another one, through the system's
 * file pickers: the user picks where the file goes, and the app needs no storage permission (ARCHITECTURE.md §7.8).
 */
@Composable
fun BackupRoute(backup: ProfileBackup, profileId: String, profileName: String, onSwitchTo: (String) -> Unit, onBack: () -> Unit) {
    val resolver = LocalContext.current.applicationContext.contentResolver
    val viewModel: BackupViewModel = viewModel(
        key = "backup-$profileId",
        factory = viewModelFactory { initializer { BackupViewModel(backup, profileId, profileName) } },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(ZIP)) { uri ->
        uri?.let { viewModel.export { resolver.openOutputStream(it, "wt") } }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.import { resolver.openInputStream(it) } }
    }
    BackupScreen(
        state = state,
        profileName = profileName,
        onExport = { exportLauncher.launch(viewModel.suggestedFileName()) },
        // Some file managers label zips as plain binary data, so both are offered.
        onImport = { importLauncher.launch(arrayOf(ZIP, "application/octet-stream", "application/x-zip-compressed")) },
        onSwitchTo = onSwitchTo,
        onBack = onBack,
    )
}

private const val ZIP = "application/zip"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    state: BackupUiState,
    profileName: String,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onSwitchTo: (String) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.backup_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.backup_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ActionCard(
                title = stringResource(R.string.backup_export_title, profileName),
                text = stringResource(R.string.backup_export_text),
                button = stringResource(R.string.backup_export),
                enabled = !state.working,
                onClick = onExport,
            )
            ActionCard(
                title = stringResource(R.string.backup_import_title),
                text = stringResource(R.string.backup_import_text),
                button = stringResource(R.string.backup_import),
                enabled = !state.working,
                onClick = onImport,
            )
            if (state.working) {
                Text(stringResource(R.string.backup_working), style = MaterialTheme.typography.bodyMedium)
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            state.message?.let { Result(it, onSwitchTo) }
        }
    }
}

@Composable
private fun ActionCard(title: String, text: String, button: String, enabled: Boolean, onClick: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FilledTonalButton(onClick = onClick, enabled = enabled) { Text(button) }
        }
    }
}

@Composable
private fun Result(message: BackupMessage, onSwitchTo: (String) -> Unit) {
    val text = when (message) {
        is BackupMessage.Exported -> pluralStringResource(R.plurals.backup_exported, message.garments, message.garments)
        BackupMessage.ExportFailed -> stringResource(R.string.backup_export_failed)
        is BackupMessage.Imported -> stringResource(
            R.string.backup_imported,
            message.profile.displayName(),
            pluralStringResource(R.plurals.backup_garments, message.garments, message.garments),
            pluralStringResource(R.plurals.backup_outfits, message.outfits, message.outfits),
            pluralStringResource(R.plurals.backup_entries, message.diaryEntries, message.diaryEntries),
        )
        BackupMessage.NotAnExport -> stringResource(R.string.backup_not_an_export)
        BackupMessage.TooNew -> stringResource(R.string.backup_too_new)
        BackupMessage.ImportFailed -> stringResource(R.string.backup_import_failed)
    }
    val isError = message is BackupMessage.ExportFailed || message is BackupMessage.NotAnExport ||
        message is BackupMessage.TooNew || message is BackupMessage.ImportFailed
    Text(text, style = MaterialTheme.typography.bodyLarge, color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
    if (message is BackupMessage.Imported) {
        TextButton(onClick = { onSwitchTo(message.profile.id) }) { Text(stringResource(R.string.backup_switch)) }
    }
}
