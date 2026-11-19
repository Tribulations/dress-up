package com.dressup.feature.profiles

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dressup.core.data.backup.ProfileBackup
import com.dressup.core.data.profile.ProfileRepository

/**
 * Shown on the very first start (and after an update from v0.1.0), until the user has a profile: their name, or an
 * import of an exported profile, e.g. from their previous phone.
 */
@Composable
fun WelcomeRoute(profiles: ProfileRepository, backup: ProfileBackup) {
    val viewModel: WelcomeViewModel = viewModel(factory = viewModelFactory { initializer { WelcomeViewModel(profiles, backup) } })
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val resolver = LocalContext.current.applicationContext.contentResolver
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.import { resolver.openInputStream(it) } }
    }
    WelcomeScreen(
        state = state,
        onSubmit = viewModel::submit,
        // Some file managers label zips as plain binary data, so both are offered (as in Settings → Export & import).
        onImport = { importLauncher.launch(arrayOf("application/zip", "application/octet-stream", "application/x-zip-compressed")) },
    )
}

@Composable
private fun WelcomeScreen(state: WelcomeUiState, onSubmit: (String) -> Unit, onImport: () -> Unit) {
    val error = state.nameError
    var name by rememberSaveable { mutableStateOf("") }
    Surface(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.safeDrawingPadding().imePadding().padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(R.string.welcome_app_name),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                stringResource(R.string.welcome_tagline),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(48.dp))
            Text(stringResource(R.string.welcome_question), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.welcome_name_label)) },
                singleLine = true,
                isError = error != null,
                supportingText = error?.let { { Text(stringResource(it.textRes)) } },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSubmit(name) }),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = { onSubmit(name) }, enabled = !state.importing, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.welcome_start))
            }
            Spacer(Modifier.height(24.dp))
            Text(
                stringResource(R.string.welcome_import_hint),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onImport, enabled = !state.importing, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(if (state.importing) R.string.welcome_importing else R.string.welcome_import))
            }
            if (state.importing) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp))
            state.importError?.let { importError ->
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(importError.textRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(32.dp))
            Text(
                stringResource(R.string.welcome_test_user_hint),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val WelcomeImportError.textRes: Int
    get() = when (this) {
        WelcomeImportError.NOT_AN_EXPORT -> R.string.welcome_import_not_an_export
        WelcomeImportError.TOO_NEW -> R.string.welcome_import_too_new
        WelcomeImportError.FAILED -> R.string.welcome_import_failed
    }

internal val NameError.textRes: Int
    get() = when (this) {
        NameError.BLANK -> R.string.profiles_name_blank
        NameError.DUPLICATE -> R.string.profiles_name_duplicate
    }
