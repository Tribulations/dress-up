package com.dressup.core.data.db

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "categories")
internal data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Set for the categories the app ships with (never changes once released); null for the user's own. */
    val builtInKey: String?,
    val hidden: Boolean,
    val sortOrder: Int,
)

@Entity(tableName = "garments")
internal data class GarmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** The user's own name for it ("my favourite sweater"), or null. */
    val name: String? = null,
    val description: String? = null,
    /** [com.dressup.core.model.GarmentColour.key], or null when no colour is set. */
    val colourKey: String?,
    val photoFileName: String?,
    /** The photo with its background removed (a PNG), or null. */
    val cutoutFileName: String? = null,
    /** 1..10 half stars, or null when not rated. */
    val ratingHalfStars: Int?,
    val review: String? = null,
    val liked: Boolean,
    val createdAtMillis: Long,
)

/** A garment's categories. A garment has at least one; [position] 0 is its main category. */
@Entity(
    tableName = "garment_categories",
    primaryKeys = ["garmentId", "categoryId"],
    foreignKeys = [
        ForeignKey(entity = GarmentEntity::class, parentColumns = ["id"], childColumns = ["garmentId"], onDelete = ForeignKey.CASCADE),
        // RESTRICT: a category can't be deleted while a garment uses it, so no garment is ever left without one.
        ForeignKey(entity = CategoryEntity::class, parentColumns = ["id"], childColumns = ["categoryId"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index("categoryId")],
)
internal data class GarmentCategoryEntity(
    val garmentId: Long,
    val categoryId: Long,
    val position: Int,
)

/** One aspect rating of a garment (fit, comfort, …). No row means "not rated". */
@Entity(
    tableName = "garment_aspect_ratings",
    primaryKeys = ["garmentId", "aspectKey"],
    foreignKeys = [
        ForeignKey(entity = GarmentEntity::class, parentColumns = ["id"], childColumns = ["garmentId"], onDelete = ForeignKey.CASCADE),
    ],
)
internal data class AspectRatingEntity(
    val garmentId: Long,
    /** [com.dressup.core.model.GarmentAspect.key]. */
    val aspectKey: String,
    val halfStars: Int,
)

@Entity(tableName = "outfits")
internal data class OutfitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String?,
    val createdAtMillis: Long,
    /** 1..10 half stars, or null when not rated. */
    val ratingHalfStars: Int? = null,
    val review: String? = null,
    @ColumnInfo(defaultValue = "0") val liked: Boolean = false,
)

/** One aspect rating of an outfit ([com.dressup.core.model.OutfitAspect]). No row means "not rated". */
@Entity(
    tableName = "outfit_aspect_ratings",
    primaryKeys = ["outfitId", "aspectKey"],
    foreignKeys = [
        ForeignKey(entity = OutfitEntity::class, parentColumns = ["id"], childColumns = ["outfitId"], onDelete = ForeignKey.CASCADE),
    ],
)
internal data class OutfitAspectRatingEntity(
    val outfitId: Long,
    val aspectKey: String,
    val halfStars: Int,
)

/** A wear in the diary ([com.dressup.core.model.DiaryEntry]). */
@Entity(
    tableName = "diary_entries",
    foreignKeys = [
        // A deleted outfit leaves the entry, and the garments that were worn, in the diary.
        ForeignKey(entity = OutfitEntity::class, parentColumns = ["id"], childColumns = ["outfitId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("outfitId"), Index("dateEpochDay")],
)
internal data class DiaryEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** [java.time.LocalDate.toEpochDay]: a calendar date, with no time zone to shift it. */
    val dateEpochDay: Long,
    val outfitId: Long?,
    val occasion: String?,
    val note: String?,
    val createdAtMillis: Long,
)

/** A garment worn in a diary entry. [position] keeps the order they were picked or laid out in. */
@Entity(
    tableName = "diary_entry_garments",
    primaryKeys = ["entryId", "garmentId"],
    foreignKeys = [
        ForeignKey(entity = DiaryEntryEntity::class, parentColumns = ["id"], childColumns = ["entryId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = GarmentEntity::class, parentColumns = ["id"], childColumns = ["garmentId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("garmentId")],
)
internal data class DiaryEntryGarmentEntity(
    val entryId: Long,
    val garmentId: Long,
    val position: Int,
)

/** One garment on an outfit's board ([com.dressup.core.model.OutfitItem]). [position] is the drawing order. */
@Entity(
    tableName = "outfit_items",
    primaryKeys = ["outfitId", "garmentId"],
    foreignKeys = [
        ForeignKey(entity = OutfitEntity::class, parentColumns = ["id"], childColumns = ["outfitId"], onDelete = ForeignKey.CASCADE),
        // A deleted garment simply disappears from the outfits it was in.
        ForeignKey(entity = GarmentEntity::class, parentColumns = ["id"], childColumns = ["garmentId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("garmentId")],
)
internal data class OutfitItemEntity(
    val outfitId: Long,
    val garmentId: Long,
    val position: Int,
    val x: Float,
    val y: Float,
    val scale: Float,
)

internal data class GarmentWithDetails(
    @Embedded val garment: GarmentEntity,
    @Relation(parentColumn = "id", entityColumn = "garmentId") val categories: List<GarmentCategoryEntity>,
    @Relation(parentColumn = "id", entityColumn = "garmentId") val aspectRatings: List<AspectRatingEntity>,
)

internal data class OutfitWithItems(
    @Embedded val outfit: OutfitEntity,
    @Relation(parentColumn = "id", entityColumn = "outfitId") val items: List<OutfitItemEntity>,
    @Relation(parentColumn = "id", entityColumn = "outfitId") val aspectRatings: List<OutfitAspectRatingEntity>,
)

internal data class DiaryEntryWithGarments(
    @Embedded val entry: DiaryEntryEntity,
    @Relation(parentColumn = "id", entityColumn = "entryId") val garments: List<DiaryEntryGarmentEntity>,
)
