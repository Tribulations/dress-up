package com.dressup.core.data.profile

import android.content.Context
import com.dressup.core.data.DressUpData
import com.dressup.core.data.photo.BackgroundRemover
import com.dressup.core.data.testuser.TestWardrobe
import com.dressup.core.model.Profile
import com.dressup.core.model.ProfilesState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * The app's profiles and each profile's [DressUpData] (manual dependency injection, ARCHITECTURE.md §2). The
 * `Application` creates one instance.
 */
class DressUpProfiles(
    context: Context,
    private val backgroundRemover: BackgroundRemover = BackgroundRemover.None,
) : ProfileRepository {
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

    override suspend fun openTestUser() = testUserLock.withLock {
        if (state.value.testUser == null) {
            withContext(Dispatchers.IO) { TestWardrobe.fill(data(TEST_USER_PROFILE_ID)) }
            // Added only once it is full, so an interrupted fill is simply done again next time.
            update { it.copy(profiles = it.profiles + Profile(TEST_USER_PROFILE_ID, name = "", isTestUser = true)) }
        }
        switchTo(TEST_USER_PROFILE_ID)
    }

    override suspend fun resetTestUser() = testUserLock.withLock {
        withContext(Dispatchers.IO) { TestWardrobe.fill(data(TEST_USER_PROFILE_ID)) }
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
    }
}
