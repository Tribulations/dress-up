package com.dressup.core.data.profile

/** The id of the first profile, made on the first start. It keeps v0.1.0's file names, so an update loses nothing. */
const val MAIN_PROFILE_ID = "main"

/** The id of the built-in test user (ARCHITECTURE.md §5.1). */
const val TEST_USER_PROFILE_ID = "test"

/**
 * Where a profile's wardrobe lives: its Room database file and its photo folder inside `filesDir`. [ownDir] is the
 * folder that belongs to the profile alone, deleted with it.
 */
internal data class ProfilePaths(val databaseName: String, val photoDir: String, val ownDir: String) {
    companion object {
        fun of(profileId: String): ProfilePaths = when (profileId) {
            MAIN_PROFILE_ID -> ProfilePaths(databaseName = "dress-up.db", photoDir = "photos", ownDir = "photos")
            else -> ProfilePaths("dress-up-$profileId.db", photoDir = "profiles/$profileId/photos", ownDir = "profiles/$profileId")
        }
    }
}
