package com.dressup.core.model

/**
 * One piece of clothing in the wardrobe (ARCHITECTURE.md §5).
 *
 * Only a category is required: [categoryIds] has at least one, the first being the main one (it sorts the garment and
 * stands in for a missing [name]). [photoFileName] and [cutoutFileName] are files in the app's private photo folder;
 * the cut-out is the photo with its background removed, shown instead of the photo when there is one.
 */
data class Garment(
    val id: Long,
    val name: String?,
    val description: String?,
    val categoryIds: List<Long>,
    /** Its colours in the order picked, the first being the main one; empty when none is set. */
    val colours: List<GarmentColour>,
    val photoFileName: String?,
    val cutoutFileName: String?,
    val ratings: Ratings<GarmentAspect>,
    val createdAtMillis: Long,
) {
    /** The cut-out when there is one, otherwise the photo. */
    val pictureFileName: String? get() = cutoutFileName ?: photoFileName
}

/** What the editor saves: a new garment when [id] is null, otherwise an update of an existing one. */
data class GarmentDraft(
    val id: Long?,
    val name: String?,
    val description: String?,
    val categoryIds: List<Long>,
    val colours: List<GarmentColour>,
    val photoFileName: String?,
)

/** The longest garment name we accept, so it fits as a title. */
const val MAX_GARMENT_NAME_LENGTH = 40
const val MAX_DESCRIPTION_LENGTH = 500
const val MAX_REVIEW_LENGTH = 2000

/** Trims [text] and turns an empty one into null, so "no name" is always stored the same way. */
fun cleanText(text: String?, maxLength: Int): String? = text?.trim()?.take(maxLength)?.trim()?.ifEmpty { null }
