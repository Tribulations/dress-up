package com.dressup.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
internal interface GarmentDao {
    @Query("SELECT * FROM garments ORDER BY createdAtMillis DESC, id DESC")
    fun observeAll(): Flow<List<GarmentEntity>>

    @Query("SELECT * FROM garments WHERE id = :id")
    fun observe(id: Long): Flow<GarmentEntity?>

    @Query("SELECT * FROM garments WHERE id = :id")
    suspend fun get(id: Long): GarmentEntity?

    @Insert
    suspend fun insert(garment: GarmentEntity): Long

    @Query("UPDATE garments SET categoryId = :categoryId, colourKey = :colourKey, photoFileName = :photoFileName WHERE id = :id")
    suspend fun updateDetails(id: Long, categoryId: Long, colourKey: String?, photoFileName: String?)

    @Query("UPDATE garments SET ratingHalfStars = :halfStars WHERE id = :id")
    suspend fun setRating(id: Long, halfStars: Int?)

    @Query("UPDATE garments SET liked = :liked WHERE id = :id")
    suspend fun setLiked(id: Long, liked: Boolean)

    @Query("DELETE FROM garments WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT photoFileName FROM garments WHERE photoFileName IS NOT NULL")
    suspend fun photoFileNames(): List<String>
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

    @Query("SELECT COUNT(*) FROM garments WHERE categoryId = :id")
    suspend fun garmentCount(id: Long): Int
}
