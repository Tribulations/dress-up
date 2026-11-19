package com.dressup.core.data.profile

import com.dressup.core.model.Profile
import com.dressup.core.model.ProfilesState
import kotlinx.coroutines.flow.StateFlow

/**
 * The profiles on this phone and which one is active (ARCHITECTURE.md §5.1). Switching profile makes the app show
 * that profile's wardrobe; the app's screens are rebuilt for it.
 */
interface ProfileRepository {
    val state: StateFlow<ProfilesState>

    /** The first start's "What's your name?": creates the user's own profile and makes it active. */
    fun createFirst(name: String): ProfileNameResult

    /** Adds another profile of the user's own (not active until switched to). */
    fun add(name: String): ProfileNameResult

    /** Renames one of the user's own profiles. The test user can't be renamed. */
    fun rename(id: String, name: String): ProfileNameResult

    fun switchTo(id: String)

    /** Deletes a profile that isn't active, with its whole wardrobe. The UI must confirm first. */
    suspend fun delete(id: String): DeleteProfileResult

    /** Switches to the test user, creating its prefilled wardrobe first if it doesn't exist yet. */
    suspend fun openTestUser()

    /** Throws away all changes in the test user's wardrobe and fills it again. */
    suspend fun resetTestUser()
}

sealed interface ProfileNameResult {
    data class Saved(val profile: Profile) : ProfileNameResult
    data object Blank : ProfileNameResult
    data object Duplicate : ProfileNameResult
}

enum class DeleteProfileResult { DELETED, ACTIVE, LAST_OWN }

/** The longest profile name we accept. */
const val MAX_PROFILE_NAME_LENGTH = 30
