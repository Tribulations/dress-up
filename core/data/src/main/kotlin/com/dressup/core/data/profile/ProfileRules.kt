package com.dressup.core.data.profile

import com.dressup.core.model.ProfilesState

internal fun cleanProfileName(name: String): String =
    name.trim().replace(Regex("\\s+"), " ").take(MAX_PROFILE_NAME_LENGTH)

/** Null when [cleanName] can be used for a profile of the user's own (other than [exceptId]). */
internal fun ProfilesState.nameProblem(cleanName: String, exceptId: String? = null): ProfileNameResult? = when {
    cleanName.isEmpty() -> ProfileNameResult.Blank
    own.any { it.id != exceptId && it.name.equals(cleanName, ignoreCase = true) } -> ProfileNameResult.Duplicate
    else -> null
}

/** Null when profile [id] may be deleted: never the active one, and the user always keeps one profile of their own. */
internal fun ProfilesState.deleteProblem(id: String): DeleteProfileResult? {
    val profile = profiles.firstOrNull { it.id == id } ?: return null
    return when {
        id == activeId -> DeleteProfileResult.ACTIVE
        !profile.isTestUser && own.size <= 1 -> DeleteProfileResult.LAST_OWN
        else -> null
    }
}
