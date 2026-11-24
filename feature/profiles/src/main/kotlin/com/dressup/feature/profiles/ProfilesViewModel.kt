package com.dressup.feature.profiles

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dressup.core.data.profile.DeleteProfileResult
import com.dressup.core.data.profile.ProfileNameResult
import com.dressup.core.data.profile.ProfileRepository
import com.dressup.core.model.Profile
import com.dressup.core.ui.WhileUiSubscribed
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The open name dialog: a new profile when [profileId] is null, otherwise renaming it. */
data class ProfileNameDialog(val profileId: String?, val initialName: String, val error: NameError? = null)

enum class NameError { BLANK, DUPLICATE }

enum class ProfilesBusy { PREPARING_TEST_USER, RESETTING_TEST_USER }

enum class ProfilesMessage { CANT_DELETE_ACTIVE, CANT_DELETE_LAST, TEST_USER_RESET, TEST_USER_FAILED }

data class ProfilesUiState(
    val own: List<Profile> = emptyList(),
    /** Null until the test user has been created. */
    val testUser: Profile? = null,
    val activeId: String? = null,
    val busy: ProfilesBusy? = null,
    val dialog: ProfileNameDialog? = null,
    val confirmDelete: Profile? = null,
    val confirmReset: Boolean = false,
    val message: ProfilesMessage? = null,
)

private data class Overlay(
    val busy: ProfilesBusy? = null,
    val dialog: ProfileNameDialog? = null,
    val confirmDelete: Profile? = null,
    val confirmReset: Boolean = false,
    val message: ProfilesMessage? = null,
)

/** Switching profile rebuilds the app's screens for the new profile, so this screen simply goes away (§7.4). */
class ProfilesViewModel(private val profiles: ProfileRepository) : ViewModel() {
    private val overlay = MutableStateFlow(Overlay())

    val uiState: StateFlow<ProfilesUiState> = combine(profiles.state, overlay) { state, o ->
        ProfilesUiState(
            own = state.own,
            testUser = state.testUser,
            activeId = state.activeId,
            busy = o.busy,
            dialog = o.dialog,
            confirmDelete = o.confirmDelete,
            confirmReset = o.confirmReset,
            message = o.message,
        )
    }.stateIn(viewModelScope, WhileUiSubscribed, ProfilesUiState())

    fun select(profile: Profile) {
        if (overlay.value.busy != null || profile.id == profiles.state.value.activeId) return
        if (profile.isTestUser) openTestUser() else profiles.switchTo(profile.id)
    }

    /** Creates the test user on first use (this takes a moment: its pictures are drawn), then switches to it. */
    fun openTestUser() = runTestUserJob(ProfilesBusy.PREPARING_TEST_USER, doneMessage = null) { profiles.openTestUser() }

    fun askReset() = overlay.update { it.copy(confirmReset = true) }

    fun cancelReset() = overlay.update { it.copy(confirmReset = false) }

    fun confirmReset() {
        overlay.update { it.copy(confirmReset = false) }
        runTestUserJob(ProfilesBusy.RESETTING_TEST_USER, doneMessage = ProfilesMessage.TEST_USER_RESET) { profiles.resetTestUser() }
    }

    private fun runTestUserJob(busy: ProfilesBusy, doneMessage: ProfilesMessage?, job: suspend () -> Unit) {
        if (overlay.value.busy != null) return
        overlay.update { it.copy(busy = busy) }
        viewModelScope.launch {
            val message = try {
                job()
                doneMessage
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Test user job failed", e)
                ProfilesMessage.TEST_USER_FAILED
            }
            overlay.update { it.copy(busy = null, message = message) }
        }
    }

    fun startAdd() = overlay.update { it.copy(dialog = ProfileNameDialog(profileId = null, initialName = "")) }

    fun startRename(profile: Profile) =
        overlay.update { it.copy(dialog = ProfileNameDialog(profileId = profile.id, initialName = profile.name)) }

    fun closeDialog() = overlay.update { it.copy(dialog = null) }

    fun submitName(name: String) {
        val dialog = overlay.value.dialog ?: return
        val result = dialog.profileId?.let { profiles.rename(it, name) } ?: profiles.add(name)
        when (result) {
            is ProfileNameResult.Saved -> closeDialog()
            ProfileNameResult.Blank -> overlay.update { it.copy(dialog = dialog.copy(error = NameError.BLANK)) }
            ProfileNameResult.Duplicate -> overlay.update { it.copy(dialog = dialog.copy(error = NameError.DUPLICATE)) }
        }
    }

    fun askDelete(profile: Profile) = overlay.update { it.copy(confirmDelete = profile) }

    fun cancelDelete() = overlay.update { it.copy(confirmDelete = null) }

    fun confirmDelete() {
        val profile = overlay.value.confirmDelete ?: return
        overlay.update { it.copy(confirmDelete = null) }
        viewModelScope.launch {
            val message = when (profiles.delete(profile.id)) {
                DeleteProfileResult.DELETED -> null
                DeleteProfileResult.ACTIVE -> ProfilesMessage.CANT_DELETE_ACTIVE
                DeleteProfileResult.LAST_OWN -> ProfilesMessage.CANT_DELETE_LAST
            }
            overlay.update { it.copy(message = message) }
        }
    }

    fun messageShown() = overlay.update { it.copy(message = null) }

    private companion object {
        const val TAG = "Profiles"
    }
}
