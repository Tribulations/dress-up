package com.dressup.feature.profiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dressup.core.data.backup.ImportResult
import com.dressup.core.data.backup.ProfileBackup
import com.dressup.core.data.profile.ProfileNameResult
import com.dressup.core.data.profile.ProfileRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.InputStream

/** Why an import on the first start didn't work. */
enum class WelcomeImportError { NOT_AN_EXPORT, TOO_NEW, FAILED }

data class WelcomeUiState(
    val nameError: NameError? = null,
    val importing: Boolean = false,
    val importError: WelcomeImportError? = null,
)

/**
 * The first start: asks the user's name and makes their profile, or imports one from an export (a new phone) and
 * makes that the active profile. Either way the app then shows that profile's wardrobe.
 */
class WelcomeViewModel(private val profiles: ProfileRepository, private val backup: ProfileBackup) : ViewModel() {
    private val state = MutableStateFlow(WelcomeUiState())
    val uiState: StateFlow<WelcomeUiState> = state.asStateFlow()

    fun submit(name: String) {
        val error = when (profiles.createFirst(name)) {
            is ProfileNameResult.Saved -> null
            ProfileNameResult.Blank -> NameError.BLANK
            ProfileNameResult.Duplicate -> NameError.DUPLICATE
        }
        state.update { it.copy(nameError = error) }
    }

    /** [open] opens the file the user picked; null if it can't be. */
    fun import(open: () -> InputStream?) {
        if (state.value.importing) return
        state.update { it.copy(importing = true, importError = null) }
        viewModelScope.launch {
            val result = try {
                open()?.use { backup.import(it) } ?: ImportResult.Failed
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ImportResult.Failed
            }
            val error = when (result) {
                is ImportResult.Imported -> {
                    profiles.switchTo(result.profile.id) // the app moves on to its wardrobe
                    null
                }
                ImportResult.NotAnExport -> WelcomeImportError.NOT_AN_EXPORT
                ImportResult.TooNew -> WelcomeImportError.TOO_NEW
                ImportResult.Failed -> WelcomeImportError.FAILED
            }
            state.update { it.copy(importing = false, importError = error) }
        }
    }
}
