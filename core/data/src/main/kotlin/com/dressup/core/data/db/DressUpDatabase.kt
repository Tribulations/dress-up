package com.dressup.core.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [CategoryEntity::class, GarmentEntity::class], version = 1, exportSchema = true)
internal abstract class DressUpDatabase : RoomDatabase() {
    abstract fun garmentDao(): GarmentDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        /** [name] is the profile's database file (ProfilePaths). */
        fun create(context: Context, name: String): DressUpDatabase =
            Room.databaseBuilder(context, DressUpDatabase::class.java, name)
                .addCallback(SeedBuiltInCategories)
                .build()
    }
}

/** The categories the app ships with (ARCHITECTURE.md §5). The keys are stored, so they must never change. */
internal val BUILT_IN_CATEGORIES = listOf(
    "tshirt" to "T-shirt",
    "shirt" to "Shirt",
    "sweater" to "Sweater",
    "hoodie" to "Hoodie",
    "jacket" to "Jacket",
    "coat" to "Coat",
    "pants" to "Pants",
    "jeans" to "Jeans",
    "shorts" to "Shorts",
    "skirt" to "Skirt",
    "dress" to "Dress",
    "shoes" to "Shoes",
    "accessory" to "Accessory",
)

private object SeedBuiltInCategories : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        BUILT_IN_CATEGORIES.forEachIndexed { index, (key, name) ->
            db.execSQL(
                "INSERT INTO categories (name, builtInKey, hidden, sortOrder) VALUES (?, ?, 0, ?)",
                arrayOf<Any>(name, key, index),
            )
        }
    }
}
