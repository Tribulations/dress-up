package com.dressup.core.data.testuser

import com.dressup.core.data.DressUpData
import com.dressup.core.data.db.CategoryEntity
import com.dressup.core.data.db.GarmentEntity
import com.dressup.core.model.GarmentColour
import com.dressup.core.model.GarmentColour.BEIGE
import com.dressup.core.model.GarmentColour.BLACK
import com.dressup.core.model.GarmentColour.BLUE
import com.dressup.core.model.GarmentColour.BROWN
import com.dressup.core.model.GarmentColour.GREEN
import com.dressup.core.model.GarmentColour.GREY
import com.dressup.core.model.GarmentColour.MULTI
import com.dressup.core.model.GarmentColour.NAVY
import com.dressup.core.model.GarmentColour.ORANGE
import com.dressup.core.model.GarmentColour.PINK
import com.dressup.core.model.GarmentColour.PURPLE
import com.dressup.core.model.GarmentColour.RED
import com.dressup.core.model.GarmentColour.WHITE
import com.dressup.core.model.GarmentColour.YELLOW
import com.dressup.core.data.testuser.Pattern.CHECK
import com.dressup.core.data.testuser.Pattern.DENIM
import com.dressup.core.data.testuser.Pattern.PLAIN
import com.dressup.core.data.testuser.Pattern.RIB
import com.dressup.core.data.testuser.Pattern.STRIPES
import com.dressup.core.data.testuser.Shape.BEANIE
import com.dressup.core.data.testuser.Shape.BOOTS
import com.dressup.core.data.testuser.Shape.COAT
import com.dressup.core.data.testuser.Shape.DRESS
import com.dressup.core.data.testuser.Shape.HOODIE
import com.dressup.core.data.testuser.Shape.JACKET
import com.dressup.core.data.testuser.Shape.PANTS
import com.dressup.core.data.testuser.Shape.SCARF
import com.dressup.core.data.testuser.Shape.SHIRT
import com.dressup.core.data.testuser.Shape.SHORTS
import com.dressup.core.data.testuser.Shape.SKIRT
import com.dressup.core.data.testuser.Shape.SNEAKERS
import com.dressup.core.data.testuser.Shape.SWEATER
import com.dressup.core.data.testuser.Shape.TEE

/** One garment of the test user's wardrobe. [category] is a built-in key or one of [OWN_CATEGORIES]. */
internal data class TestGarment(
    val category: String,
    val colour: GarmentColour?,
    val picture: Picture?,
    val halfStars: Int? = null,
    val liked: Boolean = false,
    val daysAgo: Int,
)

/**
 * The test user's prefilled wardrobe (ARCHITECTURE.md §5.1): made-up garments with drawn pictures, a mix of rated
 * and unrated, liked, with and without colour or photo, spread over the past year, so every screen has something to
 * show. Nothing here is a real photo (GUIDELINES.md §4).
 */
internal object TestWardrobe {
    const val SPORTSWEAR = "Sportswear"
    const val NIGHTWEAR = "Nightwear"
    val OWN_CATEGORIES = listOf(SPORTSWEAR, NIGHTWEAR)

    private const val SAND = 0xFFD9CFC2.toInt()
    private const val STONE = 0xFFB8BEC2.toInt()
    private const val SAGE = 0xFFA9B8A6.toInt()
    private const val BLUSH = 0xFFE3C9C3.toInt()
    private const val SLATE = 0xFF545663.toInt()
    private const val PLUM = 0xFF4A3F52.toInt()
    private const val DENIM_BLUE = 0xFF41618A.toInt()
    private const val CAMEL = 0xFFB0814F.toInt()
    private const val MUSTARD_STITCH = 0xFFD6A13E.toInt()

    val garments: List<TestGarment> = listOf(
        TestGarment("tshirt", WHITE, Picture(TEE, colour(WHITE), backdrop = SLATE), 9, liked = true, daysAgo = 3),
        TestGarment("tshirt", BLACK, Picture(TEE, colour(BLACK), backdrop = SAND), 8, daysAgo = 40),
        TestGarment("tshirt", GREEN, Picture(TEE, colour(GREEN), backdrop = SAND), 6, daysAgo = 75),
        TestGarment("tshirt", MULTI, Picture(TEE, colour(WHITE), STRIPES, accent = colour(NAVY), backdrop = BLUSH), 7, daysAgo = 120),
        TestGarment("shirt", BLUE, Picture(SHIRT, 0xFF8FB3DE.toInt(), backdrop = SAND), 8, daysAgo = 12),
        TestGarment("shirt", WHITE, Picture(SHIRT, colour(WHITE), backdrop = PLUM), 7, liked = true, daysAgo = 200),
        TestGarment("shirt", RED, Picture(SHIRT, colour(RED), CHECK, accent = colour(BLACK), backdrop = STONE), null, daysAgo = 260),
        TestGarment("sweater", BEIGE, Picture(SWEATER, colour(BEIGE), RIB, backdrop = SLATE), 10, liked = true, daysAgo = 30),
        TestGarment("sweater", GREY, Picture(SWEATER, colour(GREY), RIB, backdrop = BLUSH), 6, daysAgo = 300),
        TestGarment("hoodie", GREY, Picture(HOODIE, 0xFF9A9A9A.toInt(), backdrop = SAGE), 5, daysAgo = 90),
        TestGarment("hoodie", PURPLE, Picture(HOODIE, colour(PURPLE), backdrop = SAND), null, daysAgo = 5),
        TestGarment("jacket", BLUE, Picture(JACKET, DENIM_BLUE, DENIM, accent = MUSTARD_STITCH, backdrop = SAND), 9, liked = true, daysAgo = 150),
        TestGarment("jacket", BLACK, Picture(JACKET, colour(BLACK), backdrop = STONE), 8, daysAgo = 330),
        TestGarment("coat", BROWN, Picture(COAT, CAMEL, backdrop = SLATE), 10, liked = true, daysAgo = 360),
        TestGarment("coat", NAVY, Picture(COAT, colour(NAVY), backdrop = SAND), 7, daysAgo = 250),
        TestGarment("pants", BEIGE, Picture(PANTS, colour(BEIGE), backdrop = PLUM), 7, daysAgo = 60),
        TestGarment("pants", BLACK, Picture(PANTS, colour(BLACK), backdrop = BLUSH), 8, daysAgo = 180),
        TestGarment("jeans", BLUE, Picture(PANTS, DENIM_BLUE, DENIM, accent = MUSTARD_STITCH, backdrop = SAND), 9, liked = true, daysAgo = 20),
        TestGarment("jeans", BLACK, Picture(PANTS, 0xFF2E2E33.toInt(), DENIM, accent = colour(GREY), backdrop = SAGE), 6, daysAgo = 220),
        TestGarment("shorts", NAVY, Picture(SHORTS, colour(NAVY), backdrop = SAND), 4, daysAgo = 100),
        TestGarment("shorts", BEIGE, picture = null, daysAgo = 110),
        TestGarment("skirt", RED, Picture(SKIRT, colour(RED), backdrop = STONE), 6, daysAgo = 140),
        TestGarment("dress", GREEN, Picture(DRESS, colour(GREEN), backdrop = BLUSH), 8, liked = true, daysAgo = 45),
        TestGarment("dress", PINK, Picture(DRESS, colour(PINK), backdrop = SLATE), null, daysAgo = 8),
        TestGarment("shoes", WHITE, Picture(SNEAKERS, colour(WHITE), accent = colour(WHITE), backdrop = SAGE), 10, liked = true, daysAgo = 15),
        TestGarment("shoes", BROWN, Picture(BOOTS, colour(BROWN), accent = 0xFF3A2A1E.toInt(), backdrop = SAND), 8, daysAgo = 280),
        TestGarment("shoes", RED, Picture(SNEAKERS, colour(RED), accent = colour(WHITE), backdrop = STONE), null, daysAgo = 1),
        TestGarment("accessory", YELLOW, Picture(BEANIE, colour(YELLOW), RIB, backdrop = PLUM), 7, daysAgo = 130),
        TestGarment("accessory", MULTI, Picture(SCARF, colour(RED), CHECK, accent = colour(NAVY), backdrop = SAND), 9, liked = true, daysAgo = 310),
        TestGarment("accessory", null, picture = null, halfStars = 3, daysAgo = 340),
        TestGarment(SPORTSWEAR, ORANGE, Picture(TEE, colour(ORANGE), STRIPES, accent = colour(BLACK), backdrop = SLATE), 5, daysAgo = 25),
        TestGarment(NIGHTWEAR, null, Picture(PANTS, 0xFF8C9CC4.toInt(), CHECK, accent = colour(WHITE), backdrop = BLUSH), null, daysAgo = 70),
    )

    /** Replaces everything in [data] with the test wardrobe. Blocking: call off the main thread. */
    suspend fun fill(data: DressUpData, now: Long = System.currentTimeMillis()) {
        val garmentDao = data.database.garmentDao()
        val categoryDao = data.database.categoryDao()
        garmentDao.deleteAll()
        categoryDao.deleteOwn()
        categoryDao.showAll()
        data.photos.deleteAllExcept(emptySet())

        OWN_CATEGORIES.forEach { categoryDao.insert(CategoryEntity(name = it, builtInKey = null, hidden = false, sortOrder = 0)) }
        val categoryIds = categoryDao.getAll().associate { (it.builtInKey ?: it.name) to it.id }

        // Oldest first, so the ids follow the dates like a wardrobe built up over time.
        garments.sortedByDescending { it.daysAgo }.forEach { garment ->
            val photo = garment.picture?.let { picture ->
                val bitmap = TestPicturePainter.paint(picture)
                data.photoFiles.save(bitmap).also { bitmap.recycle() }
            }
            garmentDao.insert(
                GarmentEntity(
                    categoryId = categoryIds.getValue(garment.category),
                    colourKey = garment.colour?.key,
                    photoFileName = photo,
                    ratingHalfStars = garment.halfStars,
                    liked = garment.liked,
                    createdAtMillis = now - garment.daysAgo * DAY_MILLIS,
                ),
            )
        }
    }

    private fun colour(colour: GarmentColour): Int = colour.argb.toInt()

    private const val DAY_MILLIS = 24L * 60 * 60 * 1000
}
