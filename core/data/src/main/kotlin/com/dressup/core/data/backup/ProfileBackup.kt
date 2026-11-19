package com.dressup.core.data.backup

import com.dressup.core.model.Profile
import java.io.InputStream
import java.io.OutputStream

/**
 * Export and import of whole profiles as a zip of `profile.yaml` and an `images/` folder (ARCHITECTURE.md §7.8).
 * The streams come from the Storage Access Framework, so the user picks where the file goes and no permission is needed.
 */
interface ProfileBackup {
    /** Writes profile [profileId]'s wardrobe to [out] (not closed here). [profileName] is the name to put in the file. */
    suspend fun export(profileId: String, profileName: String, out: OutputStream): ExportResult

    /** Reads an export and adds it as a new profile, never touching the existing ones. Not made active. */
    suspend fun import(input: InputStream): ImportResult
}

sealed interface ExportResult {
    data class Done(val garments: Int, val pictures: Int) : ExportResult
    data object Failed : ExportResult
}

sealed interface ImportResult {
    data class Imported(val profile: Profile, val garments: Int, val outfits: Int, val diaryEntries: Int) : ImportResult

    /** Not a dress-up export (no `profile.yaml`, or not one we can read). */
    data object NotAnExport : ImportResult

    /** Made by a newer dress-up whose format this version can't read. */
    data object TooNew : ImportResult

    data object Failed : ImportResult
}

/** The file name the app suggests: `dress-up-anna-2026-11-19.zip`. */
fun exportFileName(profileName: String, date: java.time.LocalDate): String {
    val slug = profileName.lowercase().replace(Regex("[^\\p{L}\\p{N}]+"), "-").trim('-').take(30).ifEmpty { "profile" }
    return "dress-up-$slug-$date.zip"
}
