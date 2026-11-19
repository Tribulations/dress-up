package com.dressup.feature.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dressup.core.data.backup.ExportResult
import com.dressup.core.data.backup.ImportResult
import com.dressup.core.data.backup.ProfileBackup
import com.dressup.core.data.backup.exportFileName
import com.dressup.core.model.Profile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDate

/** The outcome of the last export or import, shown until the next one. */
sealed interface BackupMessage {
    data class Exported(val garments: Int) : BackupMessage
    data object ExportFailed : BackupMessage
    data class Imported(val profile: Profile, val garments: Int, val outfits: Int, val diaryEntries: Int) : BackupMessage
    data object NotAnExport : BackupMessage
    data object TooNew : BackupMessage
    data object ImportFailed : BackupMessage
}

data class BackupUiState(val working: Boolean = false, val message: BackupMessage? = null)

class BackupViewModel(
    private val backup: ProfileBackup,
    private val profileId: String,
    private val profileName: String,
    private val today: () -> LocalDate = LocalDate::now,
) : ViewModel() {
    private val state = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = state.asStateFlow()

    /** The name the save dialog suggests. */
    fun suggestedFileName(): String = exportFileName(profileName, today())

    /** [open] opens the file the user picked in the save dialog; null if it can't be. */
    fun export(open: () -> OutputStream?) = run(onError = BackupMessage.ExportFailed) {
        val message = open()?.use { out ->
            when (val result = backup.export(profileId, profileName, out)) {
                is ExportResult.Done -> BackupMessage.Exported(result.garments)
                ExportResult.Failed -> BackupMessage.ExportFailed
            }
        }
        message ?: BackupMessage.ExportFailed
    }

    /** [open] opens the file the user picked; null if it can't be. */
    fun import(open: () -> InputStream?) = run(onError = BackupMessage.ImportFailed) {
        val result = open()?.use { backup.import(it) } ?: ImportResult.Failed
        when (result) {
            is ImportResult.Imported -> BackupMessage.Imported(result.profile, result.garments, result.outfits, result.diaryEntries)
            ImportResult.NotAnExport -> BackupMessage.NotAnExport
            ImportResult.TooNew -> BackupMessage.TooNew
            ImportResult.Failed -> BackupMessage.ImportFailed
        }
    }

    /** Runs one export or import at a time; [onError] if opening the file throws (e.g. it was deleted meanwhile). */
    private fun run(onError: BackupMessage, work: suspend () -> BackupMessage) {
        if (state.value.working) return
        state.update { it.copy(working = true, message = null) }
        viewModelScope.launch {
            val message = try {
                work()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onError
            }
            state.update { BackupUiState(working = false, message = message) }
        }
    }
}
