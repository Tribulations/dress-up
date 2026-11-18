package com.dressup.core.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
internal data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Set for the categories the app ships with (never changes once released); null for the user's own. */
    val builtInKey: String?,
    val hidden: Boolean,
    val sortOrder: Int,
)

@Entity(
    tableName = "garments",
    foreignKeys = [
        // RESTRICT: a category can't be deleted while a garment uses it, so no garment is ever left without one.
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("categoryId")],
)
internal data class GarmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    /** [com.dressup.core.model.GarmentColour.key], or null when no colour is set. */
    val colourKey: String?,
    val photoFileName: String?,
    /** 1..10 half stars, or null when not rated. */
    val ratingHalfStars: Int?,
    val liked: Boolean,
    val createdAtMillis: Long,
)
