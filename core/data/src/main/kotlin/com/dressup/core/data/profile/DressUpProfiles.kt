package com.dressup.core.data.profile

import android.content.Context
import android.util.Log
import com.dressup.core.data.DressUpData
import com.dressup.core.data.backup.ExportResult
import com.dressup.core.data.backup.ImportResult
import com.dressup.core.data.backup.ProfileBackup
import com.dressup.core.data.backup.ProfileExporter
import com.dressup.core.data.backup.ProfileImporter
import com.dressup.core.data.photo.BackgroundRemover
import com.dressup.core.data.testuser.TestWardrobe
import com.dressup.core.model.Profile
import com.dressup.core.model.ProfilesState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant
import java.util.UUID

/**
 * The app's profiles and each profile's [DressUpData] (manual dependency injection, ARCHITECTURE.md §2). The
 * `Application` creates one instance.
 */
class DressUpProfiles(
    context: Context,
    private val backgroundRemover: BackgroundRemover = BackgroundRemover.None,
    /** Written into exports, so a file says which version made it. */
    private val appVersion: String? = null,
) : ProfileRepository, ProfileBackup {
    private val appContext = context.applicationContext
    private val store = ProfileStore(appContext)
    private val mutableState = MutableStateFlow(store.load())
    private val openData = HashMap<String, DressUpData>()
    private val testUserLock = Mutex()

    override val state: StateFlow<ProfilesState> = mutableState.asStateFlow()

    /** The data layer of profile [profileId], opened on first use and kept open while the app runs. */
    fun data(profileId: String): DressUpData = synchronized(openData) {
        openData.getOrPut(profileId) { DressUpData(appContext, ProfilePaths.of(profileId), backgroundRemover) }
    }

    override fun createFirst(name: String): ProfileNameResult {
        if (state.value.own.isNotEmpty()) return add(name)
        return addOwn(name, id = MAIN_PROFILE_ID, activate = true)
    }

    override fun add(name: String): ProfileNameResult = addOwn(name, id = newId(), activate = false)

    private fun addOwn(name: String, id: String, activate: Boolean): ProfileNameResult {
        val clean = cleanProfileName(name)
        state.value.nameProblem(clean)?.let { return it }
        val profile = Profile(id = id, name = clean, isTestUser = false)
        update { it.copy(profiles = it.profiles + profile, activeId = if (activate) id else it.activeId) }
        return ProfileNameResult.Saved(profile)
    }

    override fun rename(id: String, name: String): ProfileNameResult {
        val profile = state.value.own.firstOrNull { it.id == id } ?: return ProfileNameResult.Blank
        val clean = cleanProfileName(name)
        state.value.nameProblem(clean, exceptId = id)?.let { return it }
        val renamed = profile.copy(name = clean)
        update { s -> s.copy(profiles = s.profiles.map { if (it.id == id) renamed else it }) }
        return ProfileNameResult.Saved(renamed)
    }

    override fun switchTo(id: String) {
        if (state.value.profiles.none { it.id == id }) return
        update { it.copy(activeId = id) }
    }

    override suspend fun delete(id: String): DeleteProfileResult {
        state.value.deleteProblem(id)?.let { return it }
        // Off the list first: if we stop half-way, what's left is unused files, never a profile with half a wardrobe.
        update { s -> s.copy(profiles = s.profiles.filterNot { it.id == id }) }
        synchronized(openData) { openData.remove(id) }?.close()
        withContext(Dispatchers.IO) {
            val paths = ProfilePaths.of(id)
            appContext.deleteDatabase(paths.databaseName)
            File(appContext.filesDir, paths.ownDir).deleteRecursively()
        }
        return DeleteProfileResult.DELETED
    }

    // NonCancellable: leaving the screen while the wardrobe is being filled must not stop it half-way.
    override suspend fun openTestUser() = testUserLock.withLock {
        if (state.value.testUser == null) {
            withContext(Dispatchers.IO + NonCancellable) { TestWardrobe.fill(data(TEST_USER_PROFILE_ID)) }
            // Added only once it is full, so an interrupted fill is simply done again next time.
            update { it.copy(profiles = it.profiles + Profile(TEST_USER_PROFILE_ID, name = "", isTestUser = true)) }
        }
        switchTo(TEST_USER_PROFILE_ID)
    }

    override suspend fun resetTestUser() = testUserLock.withLock {
        withContext(Dispatchers.IO + NonCancellable) { TestWardrobe.fill(data(TEST_USER_PROFILE_ID)) }
    }

    override suspend fun export(profileId: String, profileName: String, out: OutputStream): ExportResult =
        withContext(Dispatchers.IO) {
            try {
                ProfileExporter.export(data(profileId), profileName, appVersion, out, Instant.now())
            } catch (e: Exception) {
                Log.w(TAG, "Export failed", e)
                ExportResult.Failed
            }
        }

    override suspend fun import(input: InputStream): ImportResult = withContext(Dispatchers.IO) {
        val id = newId()
        val outcome = try {
            withContext(NonCancellable) { ProfileImporter.import(input, data(id), System.currentTimeMillis()) }
        } catch (e: Exception) {
            Log.w(TAG, "Import failed", e)
            null
        }
        if (outcome !is ProfileImporter.Outcome.Read) {
            discard(id)
            return@withContext when (outcome) {
                ProfileImporter.Outcome.NotAnExport -> ImportResult.NotAnExport
                ProfileImporter.Outcome.TooNew -> ImportResult.TooNew
                else -> ImportResult.Failed
            }
        }
        // Added to the list only once it is complete, like the test user: a failed import leaves no half profile.
        var profile: Profile? = null
        update { state ->
            Profile(id, state.uniqueName(outcome.profileName, fallback = IMPORTED_NAME), isTestUser = false).let {
                profile = it
                state.copy(profiles = state.profiles + it)
            }
        }
        ImportResult.Imported(profile!!, outcome.garments, outcome.outfits, outcome.diaryEntries)
    }

    /** Removes a profile's files that never made it onto the list. */
    private fun discard(id: String) {
        synchronized(openData) { openData.remove(id) }?.close()
        val paths = ProfilePaths.of(id)
        appContext.deleteDatabase(paths.databaseName)
        File(appContext.filesDir, paths.ownDir).deleteRecursively()
    }

    private fun update(change: (ProfilesState) -> ProfilesState) {
        synchronized(store) {
            mutableState.update(change)
            store.save(mutableState.value)
        }
    }

    private fun newId(): String = UUID.randomUUID().toString().replace("-", "").take(ID_LENGTH)

    private companion object {
        const val ID_LENGTH = 12
        const val TAG = "DressUpProfiles"
        const val IMPORTED_NAME = "Imported"
    }
}
