package com.dressup.core.data.photo

import android.graphics.Bitmap

/**
 * Cuts a garment out of its photo, so only the garment is left on a transparent background (ARCHITECTURE.md §7.5).
 *
 * An optional adapter (GUIDELINES.md §2.2): the implementation lives in `:adapter:cutout-mlkit` and is wired in `app`.
 * When the build switches it off, [None] is used and the app hides the "Remove background" button.
 */
interface BackgroundRemover {
    /** False when this build has no background removal at all. */
    val isAvailable: Boolean

    /** [photo] with its background made transparent, the same size as [photo]. Runs on the device only. */
    suspend fun removeBackground(photo: Bitmap): BackgroundRemoval

    data object None : BackgroundRemover {
        override val isAvailable = false
        override suspend fun removeBackground(photo: Bitmap) = BackgroundRemoval.Unavailable
    }
}

sealed interface BackgroundRemoval {
    data class Done(val cutout: Bitmap) : BackgroundRemoval

    /** No garment (no foreground subject) was found in the photo. */
    data object NothingFound : BackgroundRemoval

    /** The phone can't do it (e.g. no Google Play services, or its model couldn't be downloaded). */
    data object Unavailable : BackgroundRemoval

    data class Failed(val cause: Throwable) : BackgroundRemoval
}
