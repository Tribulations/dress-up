package com.dressup.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
internal interface GarmentDao {
    @Transaction
    @Query("SELECT * FROM garments ORDER BY createdAtMillis DESC, id DESC")
    fun observeAll(): Flow<List<GarmentWithDetails>>

    @Transaction
    @Query("SELECT * FROM garments WHERE id = :id")
    fun observe(id: Long): Flow<GarmentWithDetails?>

    @Query("SELECT * FROM garments WHERE id = :id")
    suspend fun get(id: Long): GarmentEntity?

    @Insert
    suspend fun insert(garment: GarmentEntity): Long

    @Query(
        "UPDATE garments SET name = :name, description = :description, colourKey = :colourKey, " +
            "photoFileName = :photoFileName WHERE id = :id",
    )
    suspend fun updateDetails(id: Long, name: String?, description: String?, colourKey: String?, photoFileName: String?)

    @Query("UPDATE garments SET cutoutFileName = :fileName WHERE id = :id")
    suspend fun setCutout(id: Long, fileName: String?)

    @Query("UPDATE garments SET ratingHalfStars = :halfStars WHERE id = :id")
    suspend fun setRating(id: Long, halfStars: Int?)

    @Query("UPDATE garments SET review = :review WHERE id = :id")
    suspend fun setReview(id: Long, review: String?)

    @Query("UPDATE garments SET liked = :liked WHERE id = :id")
    suspend fun setLiked(id: Long, liked: Boolean)

    @Query("DELETE FROM garments WHERE id = :id")
    suspend fun delete(id: Long)

    /** Every photo and cut-out file some garment points at. */
    @Query(
        "SELECT photoFileName FROM garments WHERE photoFileName IS NOT NULL " +
            "UNION SELECT cutoutFileName FROM garments WHERE cutoutFileName IS NOT NULL",
    )
    suspend fun pictureFileNames(): List<String>

    @Query("DELETE FROM garment_categories WHERE garmentId = :garmentId")
    suspend fun deleteCategories(garmentId: Long)

    @Insert
    suspend fun insertCategories(links: List<GarmentCategoryEntity>)

    @Upsert
    suspend fun upsertAspectRating(rating: AspectRatingEntity)

    @Query("DELETE FROM garment_aspect_ratings WHERE garmentId = :garmentId AND aspectKey = :aspectKey")
    suspend fun deleteAspectRating(garmentId: Long, aspectKey: String)

    /** Only for refilling the test user's wardrobe ([com.dressup.core.data.testuser.TestWardrobe]). */
    @Query("DELETE FROM garments")
    suspend fun deleteAll()
}

@Dao
internal interface CategoryDao {
    /** Built-in categories first, in their shipped order, then the user's own alphabetically. */
    @Query("SELECT * FROM categories ORDER BY builtInKey IS NULL, sortOrder, name COLLATE NOCASE")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun get(id: Long): CategoryEntity?

    @Query("SELECT COUNT(*) FROM categories WHERE name = :name COLLATE NOCASE AND id != :exceptId")
    suspend fun countWithName(name: String, exceptId: Long): Int

    @Insert
    suspend fun insert(category: CategoryEntity): Long

    @Query("UPDATE categories SET name = :name WHERE id = :id AND builtInKey IS NULL")
    suspend fun rename(id: Long, name: String)

    @Query("UPDATE categories SET hidden = :hidden WHERE id = :id")
    suspend fun setHidden(id: Long, hidden: Boolean)

    @Query("DELETE FROM categories WHERE id = :id AND builtInKey IS NULL")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM garment_categories WHERE categoryId = :id")
    suspend fun garmentCount(id: Long): Int

    @Query("SELECT * FROM categories")
    suspend fun getAll(): List<CategoryEntity>

    /** Only for refilling the test user's wardrobe: removes the user's own categories (garments must be gone). */
    @Query("DELETE FROM categories WHERE builtInKey IS NULL")
    suspend fun deleteOwn()

    @Query("UPDATE categories SET hidden = 0")
    suspend fun showAll()
}

@Dao
internal interface OutfitDao {
    @Transaction
    @Query("SELECT * FROM outfits ORDER BY createdAtMillis DESC, id DESC")
    fun observeAll(): Flow<List<OutfitWithItems>>

    @Transaction
    @Query("SELECT * FROM outfits WHERE id = :id")
    fun observe(id: Long): Flow<OutfitWithItems?>

    @Insert
    suspend fun insert(outfit: OutfitEntity): Long

    @Query("UPDATE outfits SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String?)

    @Query("DELETE FROM outfit_items WHERE outfitId = :outfitId")
    suspend fun deleteItems(outfitId: Long)

    @Insert
    suspend fun insertItems(items: List<OutfitItemEntity>)

    @Query("DELETE FROM outfits WHERE id = :id")
    suspend fun delete(id: Long)

    /** Only for refilling the test user's wardrobe. */
    @Query("DELETE FROM outfits")
    suspend fun deleteAll()
}
