package com.dressup.core.data.backup

import com.dressup.core.data.db.AspectRatingEntity
import com.dressup.core.data.db.CategoryEntity
import com.dressup.core.data.db.DiaryEntryEntity
import com.dressup.core.data.db.DiaryEntryWithGarments
import com.dressup.core.data.db.GarmentEntity
import com.dressup.core.data.db.GarmentWithDetails
import com.dressup.core.data.db.OutfitAspectRatingEntity
import com.dressup.core.data.db.OutfitEntity
import com.dressup.core.data.db.OutfitItemEntity
import com.dressup.core.data.db.OutfitWithItems
import com.dressup.core.model.BoardColour
import com.dressup.core.model.GarmentAspect
import com.dressup.core.model.GarmentColour
import com.dressup.core.model.MAX_DESCRIPTION_LENGTH
import com.dressup.core.model.MAX_GARMENT_NAME_LENGTH
import com.dressup.core.model.MAX_NOTE_LENGTH
import com.dressup.core.model.MAX_OCCASION_LENGTH
import com.dressup.core.model.MAX_OUTFIT_NAME_LENGTH
import com.dressup.core.model.MAX_REVIEW_LENGTH
import com.dressup.core.model.OutfitAspect
import com.dressup.core.model.OutfitItem
import com.dressup.core.model.Rating
import com.dressup.core.model.cleanText
import com.dressup.core.model.normalizedRotation
import java.time.Instant
import java.time.LocalDate
import kotlin.math.roundToInt

/*
 * The one place where the database's rows and `profile.yaml` meet (ARCHITECTURE.md §7.8). A new field on a garment,
 * an outfit or a diary entry is added to ProfileFile and mapped here, both ways; the exporter, the importer and the
 * test user then all pick it up.
 *
 * Reading a file is forgiving, as it may have been edited by hand: text is cleaned, numbers are clamped, and unknown
 * keys are dropped.
 */

// Rows → file.

internal fun CategoryEntity.toFile() = CategoryFile(id = id, name = name, builtIn = builtInKey, hidden = hidden)

internal fun GarmentWithDetails.toFile(): GarmentFile = GarmentFile(
    id = garment.id,
    name = garment.name,
    description = garment.description,
    categories = categories.sortedBy { it.position }.map { it.categoryId },
    colours = GarmentColour.listFromKeys(garment.colourKeys).map { it.key },
    photo = garment.photoFileName?.let(::zipPath),
    cutout = garment.cutoutFileName?.let(::zipPath),
    rating = stars(garment.ratingHalfStars),
    aspects = aspectStars(aspectRatings.map { it.aspectKey to it.halfStars }),
    review = garment.review,
    liked = garment.liked,
    added = isoTime(garment.createdAtMillis),
)

internal fun OutfitWithItems.toFile(): OutfitFile = OutfitFile(
    id = outfit.id,
    name = outfit.name,
    rating = stars(outfit.ratingHalfStars),
    aspects = aspectStars(aspectRatings.map { it.aspectKey to it.halfStars }),
    review = outfit.review,
    liked = outfit.liked,
    created = isoTime(outfit.createdAtMillis),
    backgroundColour = outfit.backgroundColourKey.takeIf { outfit.backgroundFileName == null },
    backgroundImage = outfit.backgroundFileName?.let(::zipPath),
    items = items.sortedBy { it.position }.map { OutfitItemFile(it.garmentId, it.x, it.y, it.scale, it.rotation, it.locked) },
)

internal fun DiaryEntryWithGarments.toFile(): DiaryFile = DiaryFile(
    date = LocalDate.ofEpochDay(entry.dateEpochDay).toString(),
    outfit = entry.outfitId,
    garments = garments.sortedBy { it.position }.map { it.garmentId },
    occasion = entry.occasion,
    note = entry.note,
    logged = isoTime(entry.createdAtMillis),
)

// File → rows. [picture] turns a picture's path in the zip into our own file name, or null when it isn't there.

internal fun GarmentFile.toEntity(picture: (String) -> String?, now: Long) = GarmentEntity(
    name = cleanText(name, MAX_GARMENT_NAME_LENGTH),
    description = cleanText(description, MAX_DESCRIPTION_LENGTH),
    colourKeys = colourKeys(this),
    photoFileName = photo?.let(picture),
    cutoutFileName = cutout?.let(picture),
    ratingHalfStars = halfStars(rating),
    review = cleanText(review, MAX_REVIEW_LENGTH),
    liked = liked,
    createdAtMillis = millis(added) ?: now,
)

internal fun GarmentFile.aspectEntities(garmentId: Long): List<AspectRatingEntity> =
    aspectHalfStars(aspects, GarmentAspect::fromKeyOrNull).map { (aspect, halfStars) -> AspectRatingEntity(garmentId, aspect.key, halfStars) }

internal fun OutfitFile.toEntity(picture: (String) -> String?, now: Long) = OutfitEntity(
    name = cleanText(name, MAX_OUTFIT_NAME_LENGTH),
    createdAtMillis = millis(created) ?: now,
    ratingHalfStars = halfStars(rating),
    review = cleanText(review, MAX_REVIEW_LENGTH),
    liked = liked,
    backgroundColourKey = BoardColour.fromKeyOrNull(backgroundColour)?.key,
    backgroundFileName = backgroundImage?.let(picture),
)

internal fun OutfitFile.aspectEntities(outfitId: Long): List<OutfitAspectRatingEntity> =
    aspectHalfStars(aspects, OutfitAspect::fromKeyOrNull).map { (aspect, halfStars) -> OutfitAspectRatingEntity(outfitId, aspect.key, halfStars) }

internal fun OutfitItemFile.toEntity(outfitId: Long, garmentId: Long, position: Int) = OutfitItemEntity(
    outfitId = outfitId,
    garmentId = garmentId,
    position = position,
    x = fraction(x),
    y = fraction(y),
    scale = if (scale.isFinite()) scale.coerceIn(OutfitItem.MIN_SCALE, OutfitItem.MAX_SCALE) else OutfitItem.DEFAULT_SCALE,
    rotation = normalizedRotation(rotation),
    locked = locked,
)

/** Null when the date can't be read: such an entry is skipped. */
internal fun DiaryFile.toEntity(outfitId: Long?, now: Long): DiaryEntryEntity? {
    val day = runCatching { LocalDate.parse(date) }.getOrNull() ?: return null
    return DiaryEntryEntity(
        dateEpochDay = day.toEpochDay(),
        outfitId = outfitId,
        occasion = cleanText(occasion, MAX_OCCASION_LENGTH),
        note = cleanText(note, MAX_NOTE_LENGTH),
        createdAtMillis = millis(logged) ?: now,
    )
}

/** The garment's colours as stored, from the list or (in older files) the single colour; unknown keys dropped. */
internal fun colourKeys(garment: GarmentFile): String? =
    GarmentColour.keysOf((garment.colours + listOfNotNull(garment.colour)).mapNotNull(GarmentColour::fromKeyOrNull))

// Values.

/** A picture's path in the zip: `images/<file>`. */
internal fun zipPath(fileName: String): String = ProfileExporter.IMAGES_DIR + fileName

internal fun isoTime(millis: Long): String = Instant.ofEpochMilli(millis).toString()

internal fun stars(halfStars: Int?): Double? = Rating.fromHalfStarsOrNull(halfStars)?.let { it.halfStars / 2.0 }

internal fun halfStars(stars: Double?): Int? =
    stars?.takeIf { it.isFinite() }?.let { Rating.fromHalfStarsOrNull((it * 2).roundToInt()) }?.halfStars

/** Aspect key → stars, sorted by key so an export reads the same every time. */
private fun aspectStars(rows: List<Pair<String, Int>>): Map<String, Double> =
    rows.sortedBy { it.first }.mapNotNull { (key, halfStars) -> stars(halfStars)?.let { key to it } }.toMap()

private fun <A> aspectHalfStars(aspects: Map<String, Double>, fromKey: (String) -> A?): List<Pair<A, Int>> =
    aspects.mapNotNull { (key, stars) ->
        val aspect = fromKey(key) ?: return@mapNotNull null
        halfStars(stars)?.let { aspect to it }
    }

private fun millis(iso: String?): Long? = iso?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }

private fun fraction(value: Float): Float = if (value.isFinite()) value.coerceIn(0f, 1f) else 0.5f
