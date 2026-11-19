package com.dressup.core.data.profile

import android.content.Context
import androidx.core.content.edit
import com.dressup.core.model.Profile
import com.dressup.core.model.ProfilesState

/**
 * The profile list and the active profile, in SharedPreferences. Writes use `commit` (synchronous) so a profile
 * exists on disk before its wardrobe is used. Ids are plain letters and digits, so a comma-separated list is safe.
 */
internal class ProfileStore(context: Context) {
    private val prefs = context.getSharedPreferences("profiles", Context.MODE_PRIVATE)

    fun load(): ProfilesState {
        val ids = prefs.getString(KEY_IDS, null)?.split(',')?.filter { it.isNotEmpty() }.orEmpty()
        val profiles = ids.map { id ->
            Profile(id = id, name = prefs.getString(nameKey(id), null).orEmpty(), isTestUser = prefs.getBoolean(testKey(id), false))
        }
        return ProfilesState(profiles, activeId = prefs.getString(KEY_ACTIVE, null)?.takeIf { id -> profiles.any { it.id == id } })
    }

    fun save(state: ProfilesState) {
        prefs.edit(commit = true) {
            clear()
            putString(KEY_IDS, state.profiles.joinToString(",") { it.id })
            state.profiles.forEach { profile ->
                putString(nameKey(profile.id), profile.name)
                putBoolean(testKey(profile.id), profile.isTestUser)
            }
            state.activeId?.let { putString(KEY_ACTIVE, it) }
        }
    }

    private fun nameKey(id: String) = "name.$id"
    private fun testKey(id: String) = "test.$id"

    private companion object {
        const val KEY_IDS = "ids"
        const val KEY_ACTIVE = "active"
    }
}
