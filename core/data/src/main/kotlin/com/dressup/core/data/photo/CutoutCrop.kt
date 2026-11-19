package com.dressup.core.data.photo

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** A rectangle of pixels: [left] and [top] inclusive, [right] and [bottom] exclusive. */
internal data class PixelBounds(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width get() = right - left
    val height get() = bottom - top
}

/**
 * The part of a cut-out worth keeping: the box around every pixel that isn't (almost) transparent, plus a small margin,
 * so the garment fills its tile and the outfit board. Null when nothing is left, i.e. no garment was found.
 *
 * [argb] is the cut-out's pixels row by row, as `Bitmap.getPixels` gives them.
 */
internal fun opaqueBounds(argb: IntArray, width: Int, height: Int, marginFraction: Float = 0.04f): PixelBounds? {
    var left = width
    var top = height
    var right = -1
    var bottom = -1
    for (y in 0 until height) {
        val row = y * width
        for (x in 0 until width) {
            if ((argb[row + x] ushr 24) > ALPHA_THRESHOLD) {
                left = min(left, x)
                right = max(right, x)
                top = min(top, y)
                bottom = max(bottom, y)
            }
        }
    }
    if (right < 0) return null
    val margin = (max(right - left, bottom - top) * marginFraction).roundToInt()
    return PixelBounds(
        left = max(0, left - margin),
        top = max(0, top - margin),
        right = min(width, right + 1 + margin),
        bottom = min(height, bottom + 1 + margin),
    )
}

/** Pixels this transparent or more are background left-overs, not garment. */
private const val ALPHA_THRESHOLD = 24
