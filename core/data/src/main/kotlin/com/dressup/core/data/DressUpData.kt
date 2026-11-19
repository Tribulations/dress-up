package com.dressup.core.data

import android.content.Context
import com.dressup.core.data.db.DressUpDatabase
import com.dressup.core.data.photo.FilePhotoStorage
import com.dressup.core.data.photo.PhotoStorage
import com.dressup.core.data.profile.ProfilePaths

/**
 * One profile's wardrobe: its database, photos and repositories (manual dependency injection, ARCHITECTURE.md §2).
 * Made by [com.dressup.core.data.profile.DressUpProfiles], which the app asks for the active profile's instance.
 */
class DressUpData internal constructor(context: Context, paths: ProfilePaths) {
    internal val database = DressUpDatabase.create(context.applicationContext, paths.databaseName)
    internal val photoFiles = FilePhotoStorage(context.applicationContext, paths.photoDir)

    val photos: PhotoStorage = photoFiles
    val garments: GarmentRepository = RoomGarmentRepository(database.garmentDao(), photos)
    val categories: CategoryRepository = RoomCategoryRepository(database.categoryDao())

    /** Run when nothing can be open in an editor (a fresh start, or just after switching profile). */
    suspend fun cleanUpUnusedPhotos() {
        photos.deleteAllExcept(database.garmentDao().photoFileNames().toSet())
    }

    internal fun close() = database.close()
}
