package com.dressup.core.model

/**
 * One piece of clothing in the wardrobe (ARCHITECTURE.md §5).
 *
 * Only [categoryId] is required. [photoFileName] is the name of a file in the app's private photo folder.
 */
data class Garment(
    val id: Long,
    val categoryId: Long,
    val colour: GarmentColour?,
    val photoFileName: String?,
    val rating: Rating?,
    val liked: Boolean,
    val createdAtMillis: Long,
)

/** What the editor saves: a new garment when [id] is null, otherwise an update of an existing one. */
data class GarmentDraft(
    val id: Long?,
    val categoryId: Long,
    val colour: GarmentColour?,
    val photoFileName: String?,
)
