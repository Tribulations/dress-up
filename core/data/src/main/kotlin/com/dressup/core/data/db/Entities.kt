package com.dressup.core.data.db

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
)
