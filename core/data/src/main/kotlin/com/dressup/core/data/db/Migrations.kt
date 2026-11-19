package com.dressup.core.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v0.1.1 → v0.1.2: garments get a name, description, review and cut-out; a garment's one category moves into
 * `garment_categories` (as its main category), and aspect ratings and outfits get their tables.
 *
 * Never drops data (GUIDELINES.md §1): the old garments table is renamed, copied and only dropped once every row and
 * category link is in the new tables. The CREATE statements are the ones Room exported in `schemas/…/2.json`.
 */
internal val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `garments` RENAME TO `garments_v1`")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `garments` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT, " +
                "`description` TEXT, `colourKey` TEXT, `photoFileName` TEXT, `cutoutFileName` TEXT, " +
                "`ratingHalfStars` INTEGER, `review` TEXT, `liked` INTEGER NOT NULL, `createdAtMillis` INTEGER NOT NULL)",
        )
        db.execSQL(
            "INSERT INTO `garments` (`id`, `colourKey`, `photoFileName`, `ratingHalfStars`, `liked`, `createdAtMillis`) " +
                "SELECT `id`, `colourKey`, `photoFileName`, `ratingHalfStars`, `liked`, `createdAtMillis` FROM `garments_v1`",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `garment_categories` (`garmentId` INTEGER NOT NULL, `categoryId` INTEGER NOT NULL, " +
                "`position` INTEGER NOT NULL, PRIMARY KEY(`garmentId`, `categoryId`), " +
                "FOREIGN KEY(`garmentId`) REFERENCES `garments`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_garment_categories_categoryId` ON `garment_categories` (`categoryId`)")
        db.execSQL(
            "INSERT INTO `garment_categories` (`garmentId`, `categoryId`, `position`) " +
                "SELECT `id`, `categoryId`, 0 FROM `garments_v1`",
        )
        db.execSQL("DROP TABLE `garments_v1`")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `garment_aspect_ratings` (`garmentId` INTEGER NOT NULL, `aspectKey` TEXT NOT NULL, " +
                "`halfStars` INTEGER NOT NULL, PRIMARY KEY(`garmentId`, `aspectKey`), " +
                "FOREIGN KEY(`garmentId`) REFERENCES `garments`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `outfits` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT, " +
                "`createdAtMillis` INTEGER NOT NULL)",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `outfit_items` (`outfitId` INTEGER NOT NULL, `garmentId` INTEGER NOT NULL, " +
                "`position` INTEGER NOT NULL, `x` REAL NOT NULL, `y` REAL NOT NULL, `scale` REAL NOT NULL, " +
                "PRIMARY KEY(`outfitId`, `garmentId`), " +
                "FOREIGN KEY(`outfitId`) REFERENCES `outfits`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(`garmentId`) REFERENCES `garments`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_outfit_items_garmentId` ON `outfit_items` (`garmentId`)")
    }
}
