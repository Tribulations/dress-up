package com.dressup.core.data.testuser

import com.dressup.core.data.DressUpData
import com.dressup.core.data.backup.CategoryFile
import com.dressup.core.data.backup.DiaryFile
import com.dressup.core.data.backup.GarmentFile
import com.dressup.core.data.backup.OutfitFile
import com.dressup.core.data.backup.OutfitItemFile
import com.dressup.core.data.backup.ProfileFile
import com.dressup.core.data.backup.ProfileImporter
import com.dressup.core.data.backup.ProfileInfo
import com.dressup.core.data.backup.isoTime
import com.dressup.core.data.backup.stars
import com.dressup.core.data.backup.zipPath
import com.dressup.core.data.db.BUILT_IN_CATEGORIES
import com.dressup.core.data.photo.opaqueBounds
import com.dressup.core.model.GarmentAspect
import com.dressup.core.model.GarmentAspect.COMFORT
import com.dressup.core.model.GarmentAspect.EASY_CARE
import com.dressup.core.model.GarmentAspect.FIT
import com.dressup.core.model.GarmentAspect.QUALITY
import com.dressup.core.model.GarmentAspect.STYLE
import android.graphics.Bitmap
import com.dressup.core.model.BoardColour
import com.dressup.core.model.OutfitAspect
import java.time.Instant
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
    /** Colours after the main [colour], e.g. the navy of a white tee with navy stripes. */
    val moreColours: List<GarmentColour> = emptyList(),
) {
    val colours: List<GarmentColour> get() = listOfNotNull(colour) + moreColours

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
        moreColours: List<GarmentColour> = emptyList(),
    ) : this(
        listOf(category) + also, colour, picture, halfStars, liked, daysAgo, name, description, review, aspects, cutout, key,
        moreColours,
    )
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

internal data class TestOutfitItem(
    val key: String,
    val x: Float,
    val y: Float,
    val scale: Float,
    val rotation: Float = 0f,
    val locked: Boolean = false,
)

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
            "tshirt", WHITE, Picture(TEE, colour(WHITE), STRIPES, accent = colour(NAVY), backdrop = BLUSH), 7, daysAgo = 120,
            name = "Breton stripes", key = "breton", moreColours = listOf(NAVY),
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
            also = listOf("jacket"), key = "flannel", moreColours = listOf(BLACK),
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
            also = listOf("tshirt"), name = "Running top", moreColours = listOf(BLACK),
        ),
        TestGarment(NIGHTWEAR, null, Picture(PANTS, 0xFF8C9CC4.toInt(), CHECK, accent = colour(WHITE), backdrop = BLUSH), null, daysAgo = 70),
    )

    val outfits: List<TestOutfit> = listOf(
        TestOutfit(
            "Sunday walk", daysAgo = 2,
            listOf(
                TestOutfitItem("camel-coat", 0.32f, 0.32f, 0.55f, locked = true),
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

    /**
     * Replaces everything in [data] with the test wardrobe. It is loaded like an import ([ProfileImporter.fill]), so
     * every Reset also tries the import. Blocking: call off the main thread.
     */
    suspend fun fill(data: DressUpData, now: Long = System.currentTimeMillis()) {
        val db = data.database
        db.diaryDao().deleteAll()
        db.outfitDao().deleteAll()
        db.garmentDao().deleteAll()
        db.categoryDao().deleteOwn()
        db.categoryDao().showAll()
        data.photos.deleteAllExcept(emptySet())

        val pictures = garments.map { paintPictures(data, it) }
        val today = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate()
        val file = toProfileFile(pictures, today, now)
        val stored = pictures.flatMap { listOfNotNull(it.photo, it.cutout) }.associateBy(::zipPath)
        ProfileImporter.fill(data, file, stored, now)
    }

    /** The pictures drawn for one garment, as the files they were saved as. */
    internal data class Pictures(val photo: String? = null, val cutout: String? = null)

    /**
     * The test wardrobe as an export file. [pictures] are the [garments]' pictures, in the same order. The garments
     * come oldest first, so their ids follow the dates like a wardrobe built up over time.
     */
    internal fun toProfileFile(pictures: List<Pictures>, today: LocalDate, now: Long): ProfileFile {
        val builtIn = BUILT_IN_CATEGORIES.toMap()
        val categoryIds = (BUILT_IN_CATEGORIES.map { it.first } + OWN_CATEGORIES).withIndex().associate { (index, key) -> key to index + 1L }
        val categories = categoryIds.map { (key, id) -> CategoryFile(id, builtIn[key] ?: key, builtIn = key.takeIf { it in builtIn }) }

        val ordered = garments.zip(pictures).sortedByDescending { (garment, _) -> garment.daysAgo }
        val garmentIds = HashMap<String, Long>()
        val garmentFiles = ordered.mapIndexed { index, (garment, picture) ->
            val id = index + 1L
            garment.key?.let { garmentIds[it] = id }
            GarmentFile(
                id = id,
                name = garment.name,
                description = garment.description,
                categories = garment.categories.map(categoryIds::getValue),
                colours = garment.colours.map { it.key },
                photo = picture.photo?.let(::zipPath),
                cutout = picture.cutout?.let(::zipPath),
                rating = stars(garment.halfStars),
                aspects = garment.aspects.map { (aspect, halfStars) -> aspect.key to stars(halfStars)!! }.toMap(),
                review = garment.review,
                liked = garment.liked,
                added = isoTime(now - garment.daysAgo * DAY_MILLIS),
            )
        }

        val outfitIds = outfits.withIndex().associate { (index, outfit) -> outfit.name to index + 1L }
        val outfitFiles = outfits.map { outfit ->
            OutfitFile(
                id = outfitIds.getValue(outfit.name),
                name = outfit.name,
                rating = stars(outfit.halfStars),
                aspects = outfit.aspects.map { (aspect, halfStars) -> aspect.key to stars(halfStars)!! }.toMap(),
                review = outfit.review,
                liked = outfit.liked,
                created = isoTime(now - outfit.daysAgo * DAY_MILLIS),
                backgroundColour = outfit.background?.key,
                items = outfit.garments.map { OutfitItemFile(garmentIds.getValue(it.key), it.x, it.y, it.scale, it.rotation, it.locked) },
            )
        }

        val diaryFiles = diary.map { entry ->
            val keys = entry.outfit?.let { name -> outfits.first { it.name == name }.garments.map { it.key } } ?: entry.garments
            DiaryFile(
                date = entry.date(today).toString(),
                outfit = entry.outfit?.let(outfitIds::getValue),
                garments = keys.map(garmentIds::getValue),
                occasion = entry.occasion,
                note = entry.note,
                logged = isoTime(now),
            )
        }

        return ProfileFile(
            format = ProfileFile.FORMAT,
            version = ProfileFile.VERSION,
            profile = ProfileInfo(TEST_USER_NAME),
            categories = categories,
            garments = garmentFiles,
            outfits = outfitFiles,
            diary = diaryFiles,
        )
    }

    private fun paintPictures(data: DressUpData, garment: TestGarment): Pictures {
        val picture = garment.picture ?: return Pictures()
        return Pictures(
            photo = TestPicturePainter.paint(picture).saveWith(data.photoFiles::save),
            cutout = if (garment.cutout) cropped(TestPicturePainter.paintCutout(picture)).saveWith(data.photoFiles::saveCutout) else null,
        )
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

    private const val TEST_USER_NAME = "Test user"
    private const val DAY_MILLIS = 24L * 60 * 60 * 1000
}
