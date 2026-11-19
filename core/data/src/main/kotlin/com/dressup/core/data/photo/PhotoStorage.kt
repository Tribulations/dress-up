package com.dressup.core.data.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Garment photos, kept in app-private storage (GUIDELINES.md §4). Imported photos are downscaled and
 * re-encoded, so a wardrobe of hundreds of garments stays small (ARCHITECTURE.md §10).
 */
interface PhotoStorage {
    /** A fresh content URI the camera app can write a new photo to. Import it with [import] afterwards. */
    fun newCaptureUri(): Uri

    /** Copies a photo (from the camera or the gallery) into the photo folder and returns its file name. */
    suspend fun import(source: Uri): Result<String>

    fun file(fileName: String): File

    suspend fun delete(fileName: String)

    /** Removes photo files no garment points at, e.g. from an editor that was left without saving. */
    suspend fun deleteAllExcept(keep: Set<String>)
}

/** [relativeDir] is the profile's photo folder inside `filesDir` (ProfilePaths). */
internal class FilePhotoStorage(private val context: Context, private val relativeDir: String) : PhotoStorage {
    private val photoDir get() = File(context.filesDir, relativeDir).apply { mkdirs() }
    private val captureDir get() = File(context.cacheDir, "captures").apply { mkdirs() }

    override fun newCaptureUri(): Uri {
        captureDir.listFiles()?.forEach { it.delete() } // only the latest capture is ever needed
        val file = File(captureDir, "capture-${UUID.randomUUID()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.photos", file)
    }

    override suspend fun import(source: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = decodeDownscaled(source)
            save(bitmap).also { bitmap.recycle() }
        }.onFailure { Log.w(TAG, "Photo import failed", it) }
    }

    /** Writes [bitmap] as a new JPEG in the photo folder and returns its file name. Blocking: call off the main thread. */
    fun save(bitmap: Bitmap): String {
        val name = "${UUID.randomUUID()}.jpg"
        File(photoDir, name).outputStream().use { out ->
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)) throw IOException("JPEG encoding failed")
        }
        return name
    }

    /** ImageDecoder applies the EXIF rotation, so photos taken in portrait stay upright. */
    private fun decodeDownscaled(source: Uri): Bitmap {
        val imageSource = ImageDecoder.createSource(context.contentResolver, source)
        return ImageDecoder.decodeBitmap(imageSource) { decoder, info, _ ->
            val longest = max(info.size.width, info.size.height)
            if (longest > MAX_EDGE_PX) {
                val scale = MAX_EDGE_PX.toFloat() / longest
                decoder.setTargetSize((info.size.width * scale).roundToInt(), (info.size.height * scale).roundToInt())
            }
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    }

    override fun file(fileName: String): File = File(photoDir, fileName)

    override suspend fun delete(fileName: String) {
        withContext(Dispatchers.IO) { file(fileName).delete() }
    }

    override suspend fun deleteAllExcept(keep: Set<String>) {
        withContext(Dispatchers.IO) {
            photoDir.listFiles()?.filter { it.name !in keep }?.forEach { it.delete() }
        }
    }

    private companion object {
        const val TAG = "PhotoStorage"
        const val MAX_EDGE_PX = 1600
        const val JPEG_QUALITY = 85
    }
}
