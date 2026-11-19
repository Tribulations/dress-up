package com.dressup.core.data.backup

import kotlinx.serialization.Serializable

/**
 * What `profile.yaml` in an export holds (ARCHITECTURE.md §7.8): one profile's whole wardrobe, readable and editable by
 * hand. Pictures are files in the zip's `images/` folder; [GarmentFile.photo] and [GarmentFile.cutout] are their paths
 * in the zip (`images/<file>`).
 *
 * The ids only link things inside the file (a garment's categories, an outfit's garments, a diary entry's outfit);
 * an import gives everything new ids. Ratings are stars, 0.5 to 5. Times are ISO 8601 in UTC, dates are `YYYY-MM-DD`.
 * Keys are never renamed: a new field gets a default, so older files still import, and [version] only goes up when
 * an old reader couldn't make sense of a new file.
 */
@Serializable
internal data class ProfileFile(
    val format: String,
    val version: Int,
    val app: String? = null,
    val exportedAt: String? = null,
    val profile: ProfileInfo,
    val categories: List<CategoryFile> = emptyList(),
    val garments: List<GarmentFile> = emptyList(),
    val outfits: List<OutfitFile> = emptyList(),
    val diary: List<DiaryFile> = emptyList(),
) {
    companion object {
        const val FORMAT = "dress-up-profile"
        const val VERSION = 1
    }
}

@Serializable
internal data class ProfileInfo(val name: String)

/** [builtIn] is the key of a category the app ships with (its [name] is then only for the reader), or null for the user's own. */
@Serializable
internal data class CategoryFile(
    val id: Long,
    val name: String,
    val builtIn: String? = null,
    val hidden: Boolean = false,
)

@Serializable
internal data class GarmentFile(
    val id: Long,
    val name: String? = null,
    val description: String? = null,
    /** Category ids, the main one first. */
    val categories: List<Long>,
    val colour: String? = null,
    val photo: String? = null,
    val cutout: String? = null,
    val rating: Double? = null,
    val aspects: Map<String, Double> = emptyMap(),
    val review: String? = null,
    val liked: Boolean = false,
    val added: String? = null,
)

@Serializable
internal data class OutfitFile(
    val id: Long,
    val name: String? = null,
    val rating: Double? = null,
    val aspects: Map<String, Double> = emptyMap(),
    val review: String? = null,
    val liked: Boolean = false,
    val created: String? = null,
    /** A board colour's key (white, sage, …), or null. */
    val backgroundColour: String? = null,
    /** The board's own background image, a path in the zip (`images/<file>`), or null. */
    val backgroundImage: String? = null,
    /** Bottom to top on the board. */
    val items: List<OutfitItemFile> = emptyList(),
)

/**
 * A garment on an outfit's board: its centre ([x], [y]) and width ([scale]) as fractions of the board, and its
 * [rotation] in degrees clockwise.
 */
@Serializable
internal data class OutfitItemFile(val garment: Long, val x: Float, val y: Float, val scale: Float, val rotation: Float = 0f)

@Serializable
internal data class DiaryFile(
    val date: String,
    val outfit: Long? = null,
    /** What was worn. */
    val garments: List<Long> = emptyList(),
    val occasion: String? = null,
    val note: String? = null,
    val logged: String? = null,
)
