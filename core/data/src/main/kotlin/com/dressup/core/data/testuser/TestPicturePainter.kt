package com.dressup.core.data.testuser

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader

internal enum class Shape { TEE, SHIRT, SWEATER, HOODIE, JACKET, COAT, PANTS, SHORTS, SKIRT, DRESS, SNEAKERS, BOOTS, BEANIE, SCARF }

internal enum class Pattern { PLAIN, STRIPES, DENIM, RIB, CHECK }

/** A drawn stand-in for a garment photo: a flat-lay [shape] in [fill] on a [backdrop]. [accent]: stripes, stitching… */
internal data class Picture(
    val shape: Shape,
    val fill: Int,
    val pattern: Pattern = Pattern.PLAIN,
    val accent: Int? = null,
    val backdrop: Int,
)

/**
 * Draws the test user's garment pictures, so the test wardrobe needs no photo files in the repo or the APK.
 *
 * Everything is drawn on a 100 × 150 unit grid (the 2:3 shape of a wardrobe tile), scaled to [WIDTH] × [HEIGHT].
 */
internal object TestPicturePainter {
    const val WIDTH = 900
    const val HEIGHT = 1350
    private const val UNITS_WIDE = 100f

    fun paint(picture: Picture): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.scale(WIDTH / UNITS_WIDE, WIDTH / UNITS_WIDE)
        drawBackdrop(canvas, picture.backdrop)
        Garment(canvas, picture).draw()
        return bitmap
    }

    private fun drawBackdrop(canvas: Canvas, colour: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = LinearGradient(0f, 0f, 0f, 150f, lighten(colour, 0.12f), darken(colour, 0.12f), Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, 100f, 150f, paint)
        paint.shader = RadialGradient(50f, 70f, 95f, Color.TRANSPARENT, 0x40000000, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, 100f, 150f, paint)
    }
}

/** Draws one garment: the outline filled with fabric, then the details (collar, pockets, seams…). */
private class Garment(private val canvas: Canvas, private val picture: Picture) {
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 0.5f
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        color = darken(picture.fill, 0.35f)
    }
    private val accent = picture.accent ?: darken(picture.fill, 0.25f)

    fun draw() {
        canvas.save()
        canvas.translate(0f, verticalOffset())
        when (picture.shape) {
            Shape.TEE -> fabric(tee()).also { neckRib() }
            Shape.SHIRT -> fabric(longTop(hem = 120f)).also { shirtDetails() }
            Shape.SWEATER -> fabric(longTop(hem = 114f)).also { band(hemY = 114f, height = 7f); neckRib() }
            Shape.HOODIE -> { hood(); fabric(longTop(hem = 116f)); hoodieDetails() }
            Shape.JACKET -> fabric(longTop(hem = 108f)).also { openFront(hem = 108f, buttons = 5) }
            Shape.COAT -> fabric(longTop(hem = 136f, flare = 4f)).also { openFront(hem = 136f, buttons = 3); pocketFlaps(118f) }
            Shape.PANTS -> fabric(pants(hem = 138f)).also { waistband(); trouserDetails() }
            Shape.SHORTS -> fabric(pants(hem = 82f)).also { waistband(); trouserDetails() }
            Shape.SKIRT -> fabric(skirt()).also { skirtDetails() }
            Shape.DRESS -> fabric(dress()).also { waistTie() }
            Shape.SNEAKERS -> shoes(boot = false)
            Shape.BOOTS -> shoes(boot = true)
            Shape.BEANIE -> beanie()
            Shape.SCARF -> fabric(scarf()).also { fringe() }
        }
        canvas.restore()
    }

    private fun verticalOffset() = when (picture.shape) {
        Shape.SNEAKERS -> 14f
        Shape.BOOTS -> 18f
        Shape.BEANIE -> 20f
        Shape.SHORTS -> 25f
        Shape.SKIRT -> 8f
        else -> 0f
    }

    // --- Outlines -----------------------------------------------------------------------------------------------

    private fun tee() = Path().apply {
        moveTo(40f, 26f); quadTo(50f, 34f, 60f, 26f)
        lineTo(73f, 29f); lineTo(90f, 46f); lineTo(81f, 57f); lineTo(71f, 50f)
        lineTo(71f, 118f); lineTo(29f, 118f); lineTo(29f, 50f)
        lineTo(19f, 57f); lineTo(10f, 46f); lineTo(27f, 29f); close()
    }

    /** A long-sleeved top whose sleeves hang beside the body; [flare] widens it towards the [hem]. */
    private fun longTop(hem: Float, flare: Float = 0f) = Path().apply {
        val cuff = minOf(hem, 112f)
        moveTo(40f, 26f); quadTo(50f, 33f, 60f, 26f)
        lineTo(73f, 29f); quadTo(84f, 34f, 86f, 48f)
        lineTo(92f, cuff - 2f); lineTo(81f, cuff); lineTo(73f, 60f)
        lineTo(73f + flare, hem); lineTo(27f - flare, hem); lineTo(27f, 60f)
        lineTo(19f, cuff); lineTo(8f, cuff - 2f); lineTo(14f, 48f)
        quadTo(16f, 34f, 27f, 29f); close()
    }

    private fun pants(hem: Float) = Path().apply {
        val spread = (hem - 18f) / 30f
        moveTo(30f, 18f); lineTo(70f, 18f)
        lineTo(70f + spread, hem); lineTo(53f, hem); lineTo(50f, minOf(60f, hem - 22f)); lineTo(47f, hem)
        lineTo(30f - spread, hem); close()
    }

    private fun skirt() = Path().apply {
        moveTo(36f, 34f); lineTo(64f, 34f); lineTo(80f, 110f)
        quadTo(50f, 118f, 20f, 110f); close()
    }

    private fun dress() = Path().apply {
        moveTo(37f, 18f); lineTo(44f, 18f); quadTo(50f, 30f, 56f, 18f); lineTo(63f, 18f)
        lineTo(64f, 40f); quadTo(61f, 54f, 62f, 62f)
        lineTo(82f, 134f); quadTo(50f, 141f, 18f, 134f)
        lineTo(38f, 62f); quadTo(39f, 54f, 36f, 40f); close()
    }

    private fun scarf(): Path {
        val first = Path().apply { addRoundRect(RectF(30f, 16f, 50f, 132f), 2f, 2f, Path.Direction.CW) }
        val second = Path().apply {
            addRoundRect(RectF(48f, 24f, 68f, 126f), 2f, 2f, Path.Direction.CW)
            transform(Matrix().apply { setRotate(-7f, 58f, 75f) })
        }
        return Path().apply { op(first, second, Path.Op.UNION) }
    }

    // --- Fabric ---------------------------------------------------------------------------------------------------

    /** Fills [outline] with the fabric colour, its pattern and soft light, and a seam line. Whole pieces cast a shadow. */
    private fun fabric(outline: Path, colour: Int = picture.fill, pattern: Pattern = picture.pattern, shadow: Boolean = true) {
        fill.shader = null
        if (shadow) {
            fill.color = 0x38000000
            canvas.save(); canvas.translate(1.2f, 1.6f); canvas.drawPath(outline, fill); canvas.restore()
        }

        canvas.save()
        canvas.clipPath(outline)
        fill.color = colour
        canvas.drawPaint(fill)
        drawPattern(pattern, colour)
        fill.shader = LinearGradient(
            0f, 0f, 100f, 0f,
            intArrayOf(0x24FFFFFF, Color.TRANSPARENT, 0x2C000000), floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP,
        )
        canvas.drawPaint(fill)
        fill.shader = null
        canvas.restore()

        line.color = darken(colour, 0.35f)
        canvas.drawPath(outline, line)
    }

    private fun drawPattern(pattern: Pattern, colour: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        when (pattern) {
            Pattern.PLAIN -> Unit
            Pattern.STRIPES -> {
                paint.color = accent
                var y = 0f
                while (y < 150f) { canvas.drawRect(0f, y, 100f, y + 3f, paint); y += 7f }
            }
            Pattern.DENIM -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 0.25f
                paint.color = withAlpha(lighten(colour, 0.35f), 0x55)
                var x = -150f
                while (x < 100f) { canvas.drawLine(x, 150f, x + 150f, 0f, paint); x += 1.1f }
            }
            Pattern.RIB -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 0.45f
                paint.color = withAlpha(darken(colour, 0.3f), 0x70)
                var x = 0f
                while (x < 100f) { canvas.drawLine(x, 0f, x, 150f, paint); x += 2.2f }
            }
            Pattern.CHECK -> {
                paint.color = withAlpha(accent, 0x66)
                var p = 0f
                while (p < 150f) {
                    canvas.drawRect(p, 0f, p + 3.5f, 150f, paint)
                    canvas.drawRect(0f, p, 100f, p + 3.5f, paint)
                    p += 11f
                }
            }
        }
    }

    // --- Details --------------------------------------------------------------------------------------------------

    private fun neckRib() {
        val rib = Paint(line).apply { strokeWidth = 1.6f; color = darken(picture.fill, 0.12f) }
        canvas.drawPath(Path().apply { moveTo(40.5f, 27f); quadTo(50f, 35f, 59.5f, 27f) }, rib)
    }

    private fun shirtDetails() {
        val collar = darken(picture.fill, 0.08f)
        fabric(Path().apply { moveTo(39f, 25f); lineTo(50f, 33f); lineTo(43f, 39f); lineTo(36f, 29f); close() }, collar, Pattern.PLAIN, shadow = false)
        fabric(Path().apply { moveTo(61f, 25f); lineTo(50f, 33f); lineTo(57f, 39f); lineTo(64f, 29f); close() }, collar, Pattern.PLAIN, shadow = false)
        canvas.drawLine(50f, 34f, 50f, 120f, line)
        buttons(x = 51.8f, from = 42f, to = 114f, count = 7)
        canvas.drawRect(56f, 48f, 65f, 58f, line)
        cuffs(112f)
    }

    private fun hood() {
        fabric(Path().apply { moveTo(36f, 31f); cubicTo(33f, 9f, 67f, 9f, 64f, 31f); close() }, darken(picture.fill, 0.15f), Pattern.PLAIN)
    }

    private fun hoodieDetails() {
        fabric(Path().apply { moveTo(36f, 88f); lineTo(64f, 88f); lineTo(68f, 106f); lineTo(32f, 106f); close() }, darken(picture.fill, 0.05f), Pattern.PLAIN, shadow = false)
        val cord = Paint(line).apply { strokeWidth = 0.8f; color = lighten(picture.fill, 0.5f) }
        canvas.drawLine(46f, 31f, 45f, 50f, cord)
        canvas.drawLine(54f, 31f, 55f, 50f, cord)
        band(hemY = 116f, height = 6f)
    }

    private fun openFront(hem: Float, buttons: Int) {
        val lapel = darken(picture.fill, 0.12f)
        fabric(Path().apply { moveTo(40f, 26f); lineTo(50f, 60f); lineTo(41f, 46f); lineTo(35f, 30f); close() }, lapel, picture.pattern, shadow = false)
        fabric(Path().apply { moveTo(60f, 26f); lineTo(50f, 60f); lineTo(59f, 46f); lineTo(65f, 30f); close() }, lapel, picture.pattern, shadow = false)
        stitched { canvas.drawLine(50f, 60f, 50f, hem, it) }
        canvas.drawLine(50f, 60f, 50f, hem, line)
        buttons(x = 53f, from = 66f, to = hem - 12f, count = buttons)
        pocketFlaps(hem - 22f)
        cuffs(minOf(hem, 112f))
    }

    private fun pocketFlaps(y: Float) {
        canvas.drawLine(32f, y, 44f, y, line)
        canvas.drawLine(56f, y, 68f, y, line)
    }

    private fun waistband() {
        fabric(Path().apply { addRect(30f, 18f, 70f, 24f, Path.Direction.CW) }, darken(picture.fill, 0.1f), picture.pattern, shadow = false)
        listOf(34f, 44f, 56f, 66f).forEach { canvas.drawLine(it, 17.5f, it, 25f, line) }
    }

    private fun trouserDetails() {
        canvas.drawPath(Path().apply { moveTo(50f, 24f); lineTo(50f, 44f); quadTo(50f, 48f, 46f, 48f) }, line)
        stitched {
            canvas.drawPath(Path().apply { moveTo(31f, 27f); quadTo(40f, 28f, 42f, 24f) }, it)
            canvas.drawPath(Path().apply { moveTo(69f, 27f); quadTo(60f, 28f, 58f, 24f) }, it)
        }
    }

    private fun skirtDetails() {
        fabric(Path().apply { addRect(36f, 34f, 64f, 40f, Path.Direction.CW) }, darken(picture.fill, 0.1f), Pattern.PLAIN, shadow = false)
        listOf(44f to 34f, 50f to 50f, 56f to 66f).forEach { (top, bottom) -> canvas.drawLine(top, 41f, bottom, 114f, line) }
    }

    private fun waistTie() {
        fabric(Path().apply { addRect(37f, 60f, 63f, 65f, Path.Direction.CW) }, darken(picture.fill, 0.15f), Pattern.PLAIN, shadow = false)
    }

    private fun band(hemY: Float, height: Float) {
        val rib = darken(picture.fill, 0.08f)
        fabric(Path().apply { addRect(27f, hemY - height, 73f, hemY, Path.Direction.CW) }, rib, Pattern.RIB, shadow = false)
    }

    private fun cuffs(y: Float) {
        canvas.drawLine(9f, y - 7f, 19.5f, y - 5f, line)
        canvas.drawLine(91f, y - 7f, 80.5f, y - 5f, line)
    }

    private fun buttons(x: Float, from: Float, to: Float, count: Int) {
        val button = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = lighten(picture.fill, 0.25f) }
        val step = (to - from) / (count - 1)
        repeat(count) { i ->
            canvas.drawCircle(x, from + i * step, 0.9f, button)
            canvas.drawCircle(x, from + i * step, 0.9f, line)
        }
    }

    private fun stitched(draw: (Paint) -> Unit) {
        draw(Paint(line).apply { strokeWidth = 0.35f; color = accent; pathEffect = DashPathEffect(floatArrayOf(1.2f, 0.8f), 0f) })
    }

    private fun shoes(boot: Boolean) {
        canvas.save()
        canvas.translate(-5f, -14f)
        canvas.scale(0.92f, 0.92f, 50f, 80f)
        shoe(boot, darken(picture.fill, 0.12f))
        canvas.restore()
        shoe(boot, picture.fill)
    }

    private fun shoe(boot: Boolean, colour: Int) {
        val upper = Path().apply {
            if (boot) {
                moveTo(18f, 92f); lineTo(18f, 42f); lineTo(43f, 42f); lineTo(46f, 70f)
            } else {
                moveTo(14f, 92f); lineTo(14f, 77f); quadTo(15f, 68f, 25f, 67f); lineTo(36f, 70f); quadTo(46f, 72f, 56f, 79f)
            }
            quadTo(78f, 83f, 86f, 87f); quadTo(91f, 90f, 89f, 93f); close()
        }
        fabric(upper, colour, Pattern.PLAIN)
        val sole = picture.accent ?: Color.WHITE
        fabric(Path().apply { addRoundRect(RectF(if (boot) 16f else 12f, 92f, 91f, 99f), 3f, 3f, Path.Direction.CW) }, sole, Pattern.PLAIN, shadow = false)
        val lace = Paint(line).apply { strokeWidth = 0.7f; color = if (boot) darken(colour, 0.4f) else lighten(colour, 0.6f) }
        if (boot) {
            for (y in listOf(48f, 54f, 60f, 66f)) canvas.drawLine(36f, y, 43.5f, y + 1f, lace)
        } else {
            for (x in listOf(40f, 44f, 48f, 52f)) canvas.drawLine(x, 70f + (x - 40f) * 0.6f, x + 4f, 73f + (x - 40f) * 0.6f, lace)
        }
        canvas.drawPath(Path().apply { moveTo(70f, 92f); quadTo(72f, 84f, 82f, 86f) }, line)
    }

    private fun beanie() {
        val pom = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = lighten(picture.fill, 0.2f) }
        canvas.drawCircle(50f, 37f, 8f, pom)
        fabric(Path().apply { moveTo(24f, 82f); cubicTo(24f, 34f, 76f, 34f, 76f, 82f); close() })
        fabric(Path().apply { addRoundRect(RectF(21f, 76f, 79f, 96f), 3f, 3f, Path.Direction.CW) }, darken(picture.fill, 0.08f), Pattern.RIB, shadow = false)
    }

    private fun fringe() {
        val strand = Paint(line).apply { strokeWidth = 0.6f; color = darken(picture.fill, 0.1f) }
        var x = 31.5f
        while (x < 49f) { canvas.drawLine(x, 132f, x, 139f, strand); x += 2f }
        canvas.save()
        canvas.rotate(-7f, 58f, 75f)
        x = 49.5f
        while (x < 67f) { canvas.drawLine(x, 126f, x, 133f, strand); x += 2f }
        canvas.restore()
    }
}

private fun darken(colour: Int, amount: Float): Int = blend(colour, Color.BLACK, amount)

private fun lighten(colour: Int, amount: Float): Int = blend(colour, Color.WHITE, amount)

private fun withAlpha(colour: Int, alpha: Int): Int = (colour and 0x00FFFFFF) or (alpha shl 24)

private fun blend(from: Int, to: Int, amount: Float): Int {
    fun channel(shift: Int) = (((from shr shift) and 0xFF) * (1 - amount) + ((to shr shift) and 0xFF) * amount).toInt()
    return Color.argb(0xFF, channel(16), channel(8), channel(0))
}
