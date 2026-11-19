package com.dressup.core.data.testuser

import com.dressup.core.data.DressUpData
import com.dressup.core.data.db.AspectRatingEntity
import com.dressup.core.data.db.CategoryEntity
import com.dressup.core.data.db.GarmentCategoryEntity
import com.dressup.core.data.db.GarmentEntity
import com.dressup.core.data.db.OutfitEntity
import com.dressup.core.data.db.OutfitItemEntity
import com.dressup.core.data.photo.opaqueBounds
import com.dressup.core.model.GarmentAspect
import com.dressup.core.model.GarmentAspect.COMFORT
import com.dressup.core.model.GarmentAspect.EASY_CARE
import com.dressup.core.model.GarmentAspect.FIT
import com.dressup.core.model.GarmentAspect.QUALITY
import com.dressup.core.model.GarmentAspect.STYLE
import android.graphics.Bitmap
import com.dressup.core.data.db.DiaryEntryEntity
import com.dressup.core.data.db.DiaryEntryGarmentEntity
import com.dressup.core.data.db.OutfitAspectRatingEntity
import com.dressup.core.model.BoardColour
import com.dressup.core.model.OutfitAspect
import java.time.LocalDate
import java.time.MonthDay
import java.time.ZoneId
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

/**
 * One garment of the test user's wardrobe. [categories] are built-in keys or [TestWardrobe.OWN_CATEGORIES], the first
 * being the main one. [key] names it for [TestOutfit]. [cutout]: whether it also has a cut-out of its picture.
 */
internal data class TestGarment(
    val categories: List<String>,
    val colour: GarmentColour?,
    val picture: Picture?,
    val halfStars: Int? = null,
    val liked: Boolean = false,
    val daysAgo: Int,
    val name: String? = null,
    val description: String? = null,
    val review: String? = null,
    val aspects: Map<GarmentAspect, Int> = emptyMap(),
    val cutout: Boolean = picture != null,
    val key: String? = null,
) {
    constructor(
        category: String,
        colour: GarmentColour?,
        picture: Picture?,
        halfStars: Int? = null,
        liked: Boolean = false,
        daysAgo: Int,
        name: String? = null,
        description: String? = null,
        review: String? = null,
        aspects: Map<GarmentAspect, Int> = emptyMap(),
        cutout: Boolean = picture != null,
        key: String? = null,
        also: List<String> = emptyList(),
    ) : this(listOf(category) + also, colour, picture, halfStars, liked, daysAgo, name, description, review, aspects, cutout, key)
}

/** A made-up outfit: the [garments] by [TestGarment.key], top to bottom of the board. */
internal data class TestOutfit(
    val name: String,
    val daysAgo: Int,
    val garments: List<TestOutfitItem>,
    val halfStars: Int? = null,
    val liked: Boolean = false,
    val review: String? = null,
    val aspects: Map<OutfitAspect, Int> = emptyMap(),
    val background: BoardColour? = null,
)

/**
 * A made-up diary entry: an [outfit] by name and/or [garments] by key. It was worn [daysAgo], or on the most recent
 * [monthDay] (e.g. Christmas Eve) when that is set.
 */
internal data class TestDiaryEntry(
    val daysAgo: Int = 0,
    val outfit: String? = null,
    val garments: List<String> = emptyList(),
    val occasion: String? = null,
    val note: String? = null,
    val monthDay: MonthDay? = null,
) {
    /** The date it was worn, seen from [today]. */
    fun date(today: LocalDate): LocalDate {
        val fixed = monthDay ?: return today.minusDays(daysAgo.toLong())
        val thisYear = fixed.atYear(today.year)
        return if (thisYear.isAfter(today)) fixed.atYear(today.year - 1) else thisYear
    }
}

internal data class TestOutfitItem(val key: String, val x: Float, val y: Float, val scale: Float, val rotation: Float = 0f)

/**
 * The test user's prefilled wardrobe (ARCHITECTURE.md §5.1): made-up garments with drawn pictures, a mix of rated
 * and unrated, liked, named and unnamed, with and without colour, photo, cut-out, review or several categories, spread
 * over the past year, plus a few outfits, so every screen has something to show. Nothing here is a real photo
 * (GUIDELINES.md §4).
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
        TestGarment(
            "tshirt", WHITE, Picture(TEE, colour(WHITE), backdrop = SLATE), 9, liked = true, daysAgo = 3,
            name = "The perfect white tee", description = "Heavy cotton, boxy fit. Bought two.",
            review = "Goes with everything and keeps its shape after a hundred washes. The one I reach for first.",
            aspects = mapOf(FIT to 9, COMFORT to 10, QUALITY to 8, EASY_CARE to 7), key = "white-tee",
        ),
        TestGarment("tshirt", BLACK, Picture(TEE, colour(BLACK), backdrop = SAND), 8, daysAgo = 40, key = "black-tee"),
        TestGarment("tshirt", GREEN, Picture(TEE, colour(GREEN), backdrop = SAND), 6, daysAgo = 75, cutout = false),
        TestGarment(
            "tshirt", MULTI, Picture(TEE, colour(WHITE), STRIPES, accent = colour(NAVY), backdrop = BLUSH), 7, daysAgo = 120,
            name = "Breton stripes", key = "breton",
        ),
        TestGarment(
            "shirt", BLUE, Picture(SHIRT, 0xFF8FB3DE.toInt(), backdrop = SAND), 8, daysAgo = 12,
            name = "Office oxford", aspects = mapOf(FIT to 7, STYLE to 8, EASY_CARE to 4),
            review = "Smart enough for meetings, but it needs ironing every single time.", key = "oxford",
        ),
        TestGarment("shirt", WHITE, Picture(SHIRT, colour(WHITE), backdrop = PLUM), 7, liked = true, daysAgo = 200, cutout = false),
        TestGarment(
            "shirt", RED, Picture(SHIRT, colour(RED), CHECK, accent = colour(BLACK), backdrop = STONE), null, daysAgo = 260,
            name = "Lumberjack flannel", description = "Brushed flannel, a size too big on purpose.",
            also = listOf("jacket"), key = "flannel",
        ),
        TestGarment(
            "sweater", BEIGE, Picture(SWEATER, colour(BEIGE), RIB, backdrop = SLATE), 10, liked = true, daysAgo = 30,
            name = "Coola favvis tröjan", description = "Chunky lambswool knit from the flea market.",
            review = "My favourite. Warm without being bulky, and everyone asks where it's from. Itches a little at the neck.",
            aspects = mapOf(FIT to 9, COMFORT to 7, QUALITY to 10, STYLE to 10, EASY_CARE to 3), key = "favourite-sweater",
        ),
        TestGarment("sweater", GREY, Picture(SWEATER, colour(GREY), RIB, backdrop = BLUSH), 6, daysAgo = 300),
        TestGarment(
            "hoodie", GREY, Picture(HOODIE, 0xFF9A9A9A.toInt(), backdrop = SAGE), 5, daysAgo = 90,
            also = listOf(SPORTSWEAR), aspects = mapOf(COMFORT to 9, QUALITY to 4),
            review = "Comfy, but pilled after a month.", key = "grey-hoodie",
        ),
        TestGarment("hoodie", PURPLE, Picture(HOODIE, colour(PURPLE), backdrop = SAND), null, daysAgo = 5),
        TestGarment(
            "jacket", BLUE, Picture(JACKET, DENIM_BLUE, DENIM, accent = MUSTARD_STITCH, backdrop = SAND), 9, liked = true,
            daysAgo = 150, name = "Denim jacket", description = "Vintage, faded just right.", key = "denim-jacket",
            aspects = mapOf(FIT to 8, STYLE to 9, QUALITY to 9),
        ),
        TestGarment("jacket", BLACK, Picture(JACKET, colour(BLACK), backdrop = STONE), 8, daysAgo = 330, cutout = false),
        TestGarment(
            "coat", BROWN, Picture(COAT, CAMEL, backdrop = SLATE), 10, liked = true, daysAgo = 360,
            name = "Camel coat", review = "Worth every krona. Ten winters and counting.", key = "camel-coat",
            aspects = mapOf(QUALITY to 10, STYLE to 9, COMFORT to 8),
        ),
        TestGarment("coat", NAVY, Picture(COAT, colour(NAVY), backdrop = SAND), 7, daysAgo = 250, cutout = false),
        TestGarment("pants", BEIGE, Picture(PANTS, colour(BEIGE), backdrop = PLUM), 7, daysAgo = 60, name = "Chinos", key = "chinos"),
        TestGarment("pants", BLACK, Picture(PANTS, colour(BLACK), backdrop = BLUSH), 8, daysAgo = 180, key = "black-pants"),
        TestGarment(
            "jeans", BLUE, Picture(PANTS, DENIM_BLUE, DENIM, accent = MUSTARD_STITCH, backdrop = SAND), 9, liked = true,
            daysAgo = 20, name = "Everyday jeans", aspects = mapOf(FIT to 10, COMFORT to 8, EASY_CARE to 9), key = "jeans",
        ),
        TestGarment("jeans", BLACK, Picture(PANTS, 0xFF2E2E33.toInt(), DENIM, accent = colour(GREY), backdrop = SAGE), 6, daysAgo = 220, cutout = false),
        TestGarment(
            "shorts", NAVY, Picture(SHORTS, colour(NAVY), backdrop = SAND), 4, daysAgo = 100,
            also = listOf(SPORTSWEAR), review = "Too short for running, too sporty for anything else.",
        ),
        TestGarment("shorts", BEIGE, picture = null, daysAgo = 110, name = "Linen shorts (no photo yet)"),
        TestGarment("skirt", RED, Picture(SKIRT, colour(RED), backdrop = STONE), 6, daysAgo = 140, key = "red-skirt"),
        TestGarment(
            "dress", GREEN, Picture(DRESS, colour(GREEN), backdrop = BLUSH), 8, liked = true, daysAgo = 45,
            name = "Midsummer dress", description = "Light cotton, pockets!", key = "green-dress",
        ),
        TestGarment("dress", PINK, Picture(DRESS, colour(PINK), backdrop = SLATE), null, daysAgo = 8, cutout = false),
        TestGarment(
            "shoes", WHITE, Picture(SNEAKERS, colour(WHITE), accent = colour(WHITE), backdrop = SAGE), 10, liked = true,
            daysAgo = 15, name = "White sneakers", also = listOf(SPORTSWEAR), key = "white-sneakers",
            aspects = mapOf(COMFORT to 10, STYLE to 8, EASY_CARE to 5),
        ),
        TestGarment(
            "shoes", BROWN, Picture(BOOTS, colour(BROWN), accent = 0xFF3A2A1E.toInt(), backdrop = SAND), 8, daysAgo = 280,
            name = "Chelsea boots", key = "boots",
        ),
        TestGarment("shoes", RED, Picture(SNEAKERS, colour(RED), accent = colour(WHITE), backdrop = STONE), null, daysAgo = 1, cutout = false),
        TestGarment(
            "accessory", YELLOW, Picture(BEANIE, colour(YELLOW), RIB, backdrop = PLUM), 7, daysAgo = 130,
            name = "Mustard beanie", key = "beanie",
        ),
        TestGarment(
            "accessory", MULTI, Picture(SCARF, colour(RED), CHECK, accent = colour(NAVY), backdrop = SAND), 9, liked = true,
            daysAgo = 310, name = "Tartan scarf", key = "scarf",
        ),
        TestGarment("accessory", null, picture = null, halfStars = 3, daysAgo = 340),
        TestGarment(
            SPORTSWEAR, ORANGE, Picture(TEE, colour(ORANGE), STRIPES, accent = colour(BLACK), backdrop = SLATE), 5, daysAgo = 25,
            also = listOf("tshirt"), name = "Running top",
        ),
        TestGarment(NIGHTWEAR, null, Picture(PANTS, 0xFF8C9CC4.toInt(), CHECK, accent = colour(WHITE), backdrop = BLUSH), null, daysAgo = 70),
    )

    val outfits: List<TestOutfit> = listOf(
        TestOutfit(
            "Sunday walk", daysAgo = 2,
            listOf(
                TestOutfitItem("camel-coat", 0.32f, 0.32f, 0.55f),
                TestOutfitItem("favourite-sweater", 0.68f, 0.26f, 0.45f),
                TestOutfitItem("scarf", 0.72f, 0.55f, 0.3f, rotation = -18f),
                TestOutfitItem("jeans", 0.35f, 0.72f, 0.42f),
                TestOutfitItem("boots", 0.72f, 0.85f, 0.36f),
            ),
            halfStars = 9, liked = true,
            review = "Warm, cosy and it all goes together. The scarf makes it.",
            aspects = mapOf(OutfitAspect.COMFORT to 10, OutfitAspect.STYLE to 9, OutfitAspect.OCCASION to 9),
            background = BoardColour.SAGE,
        ),
        TestOutfit(
            "Office", daysAgo = 9,
            listOf(
                TestOutfitItem("oxford", 0.5f, 0.25f, 0.5f),
                TestOutfitItem("chinos", 0.42f, 0.68f, 0.42f),
                TestOutfitItem("white-sneakers", 0.75f, 0.86f, 0.32f),
            ),
            halfStars = 7,
            aspects = mapOf(OutfitAspect.COMFORT to 7, OutfitAspect.FIT to 8, OutfitAspect.OCCASION to 6),
        ),
        TestOutfit(
            "Midsummer", daysAgo = 30,
            listOf(
                TestOutfitItem("green-dress", 0.45f, 0.45f, 0.6f),
                TestOutfitItem("denim-jacket", 0.78f, 0.25f, 0.4f),
                TestOutfitItem("white-sneakers", 0.72f, 0.85f, 0.3f),
            ),
            halfStars = 8, liked = true,
            background = BoardColour.SKY,
        ),
    )

    /** A diary over the past months: outfits, single garments, occasions, notes, and some days with neither. */
    val diary: List<TestDiaryEntry> = listOf(
        TestDiaryEntry(daysAgo = 0, garments = listOf("white-tee", "jeans", "white-sneakers")),
        TestDiaryEntry(daysAgo = 1, outfit = "Office", note = "Big presentation. Felt sharp."),
        TestDiaryEntry(daysAgo = 2, outfit = "Sunday walk", occasion = "Walk by the lake"),
        TestDiaryEntry(daysAgo = 4, garments = listOf("favourite-sweater", "black-pants", "boots"), occasion = "Dinner with Sam"),
        TestDiaryEntry(daysAgo = 6, outfit = "Office"),
        TestDiaryEntry(daysAgo = 9, garments = listOf("grey-hoodie"), note = "Lazy Sunday at home."),
        TestDiaryEntry(daysAgo = 13, outfit = "Sunday walk", occasion = "Sam's birthday", note = "Got compliments on the scarf."),
        TestDiaryEntry(daysAgo = 16, outfit = "Office"),
        TestDiaryEntry(daysAgo = 21, garments = listOf("breton", "chinos", "white-sneakers"), occasion = "Museum day"),
        TestDiaryEntry(daysAgo = 27, garments = listOf("favourite-sweater", "jeans")),
        TestDiaryEntry(daysAgo = 34, outfit = "Midsummer", occasion = "Garden party"),
        TestDiaryEntry(daysAgo = 48, garments = listOf("flannel", "jeans", "boots", "beanie"), occasion = "Cabin weekend"),
        TestDiaryEntry(monthDay = MonthDay.of(12, 24), outfit = "Sunday walk", occasion = "Christmas Eve", note = "Too warm by the fire, but worth it."),
    )

    /** Replaces everything in [data] with the test wardrobe. Blocking: call off the main thread. */
    suspend fun fill(data: DressUpData, now: Long = System.currentTimeMillis()) {
        val garmentDao = data.database.garmentDao()
        val categoryDao = data.database.categoryDao()
        val outfitDao = data.database.outfitDao()
        val diaryDao = data.database.diaryDao()
        diaryDao.deleteAll()
        outfitDao.deleteAll()
        garmentDao.deleteAll()
        categoryDao.deleteOwn()
        categoryDao.showAll()
        data.photos.deleteAllExcept(emptySet())

        OWN_CATEGORIES.forEach { categoryDao.insert(CategoryEntity(name = it, builtInKey = null, hidden = false, sortOrder = 0)) }
        val categoryIds = categoryDao.getAll().associate { (it.builtInKey ?: it.name) to it.id }

        // Oldest first, so the ids follow the dates like a wardrobe built up over time.
        val garmentIds = HashMap<String, Long>()
        garments.sortedByDescending { it.daysAgo }.forEach { garment ->
            val id = insertGarment(data, garment, categoryIds, now)
            garment.key?.let { garmentIds[it] = id }
        }
        val outfitIds = HashMap<String, Long>()
        outfits.forEach { outfit ->
            val id = outfitDao.insert(
                OutfitEntity(
                    name = outfit.name,
                    createdAtMillis = now - outfit.daysAgo * DAY_MILLIS,
                    ratingHalfStars = outfit.halfStars,
                    review = outfit.review,
                    liked = outfit.liked,
                    backgroundColourKey = outfit.background?.key,
                ),
            )
            outfitIds[outfit.name] = id
            outfitDao.insertItems(
                outfit.garments.mapIndexed { position, item ->
                    OutfitItemEntity(id, garmentIds.getValue(item.key), position, item.x, item.y, item.scale, item.rotation)
                },
            )
            outfitDao.upsertAspectRatings(outfit.aspects.map { (aspect, halfStars) -> OutfitAspectRatingEntity(id, aspect.key, halfStars) })
        }
        val today = java.time.Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate()
        diary.forEach { entry ->
            val keys = entry.outfit?.let { name -> outfits.first { it.name == name }.garments.map { it.key } } ?: entry.garments
            val id = diaryDao.insert(
                DiaryEntryEntity(
                    dateEpochDay = entry.date(today).toEpochDay(),
                    outfitId = entry.outfit?.let(outfitIds::getValue),
                    occasion = entry.occasion,
                    note = entry.note,
                    createdAtMillis = now,
                ),
            )
            diaryDao.insertGarments(keys.mapIndexed { position, key -> DiaryEntryGarmentEntity(id, garmentIds.getValue(key), position) })
        }
    }

    private suspend fun insertGarment(data: DressUpData, garment: TestGarment, categoryIds: Map<String, Long>, now: Long): Long {
        val garmentDao = data.database.garmentDao()
        val photo = garment.picture?.let { picture -> TestPicturePainter.paint(picture).saveWith(data.photoFiles::save) }
        val cutout = garment.picture?.takeIf { garment.cutout }?.let { picture ->
            cropped(TestPicturePainter.paintCutout(picture)).saveWith(data.photoFiles::saveCutout)
        }
        val id = garmentDao.insert(
            GarmentEntity(
                name = garment.name,
                description = garment.description,
                colourKey = garment.colour?.key,
                photoFileName = photo,
                cutoutFileName = cutout,
                ratingHalfStars = garment.halfStars,
                review = garment.review,
                liked = garment.liked,
                createdAtMillis = now - garment.daysAgo * DAY_MILLIS,
            ),
        )
        garmentDao.insertCategories(
            garment.categories.mapIndexed { position, key -> GarmentCategoryEntity(id, categoryIds.getValue(key), position) },
        )
        garment.aspects.forEach { (aspect, halfStars) -> garmentDao.upsertAspectRating(AspectRatingEntity(id, aspect.key, halfStars)) }
        return id
    }

    /** Crops a drawn cut-out the same way a real one is cropped (RoomGarmentRepository). */
    private fun cropped(bitmap: Bitmap): Bitmap {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val bounds = opaqueBounds(pixels, bitmap.width, bitmap.height) ?: return bitmap
        return Bitmap.createBitmap(bitmap, bounds.left, bounds.top, bounds.width, bounds.height).also { bitmap.recycle() }
    }

    private fun Bitmap.saveWith(save: (Bitmap) -> String): String = save(this).also { recycle() }

    private fun colour(colour: GarmentColour): Int = colour.argb.toInt()

    private const val DAY_MILLIS = 24L * 60 * 60 * 1000
}
