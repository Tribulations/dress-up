package com.dressup.core.data

import android.content.Context
import com.dressup.core.data.db.DressUpDatabase
import com.dressup.core.data.photo.FilePhotoStorage
import com.dressup.core.data.photo.PhotoStorage

/**
 * The app's data layer in one place (manual dependency injection, ARCHITECTURE.md §2). The app creates one instance
 * and hands the repositories to the feature screens.
 */
class DressUpData(context: Context) {
    private val database = DressUpDatabase.create(context.applicationContext)

    val photos: PhotoStorage = FilePhotoStorage(context.applicationContext)
    val garments: GarmentRepository = RoomGarmentRepository(database.garmentDao(), photos)
    val categories: CategoryRepository = RoomCategoryRepository(database.categoryDao())

    /** Run once at app start, before any editor can be open: removes photos no garment uses. */
    suspend fun cleanUpUnusedPhotos() {
        photos.deleteAllExcept(database.garmentDao().photoFileNames().toSet())
    }
}
