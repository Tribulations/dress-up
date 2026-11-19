package com.dressup.adapter.cutout

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.dressup.core.data.photo.BackgroundRemoval
import com.dressup.core.data.photo.BackgroundRemover
import com.google.android.gms.common.moduleinstall.InstallStatusListener
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate.InstallState
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * [BackgroundRemover] with ML Kit subject segmentation (ARCHITECTURE.md §7.5). The photo never leaves the phone: the
 * model runs on the device. It comes with Google Play services, which downloads it the first time it is needed.
 */
class MlKitBackgroundRemover(context: Context) : BackgroundRemover {
    private val appContext = context.applicationContext
    private val segmenter by lazy {
        SubjectSegmentation.getClient(SubjectSegmenterOptions.Builder().enableForegroundBitmap().build())
    }

    override val isAvailable: Boolean = true

    override suspend fun removeBackground(photo: Bitmap): BackgroundRemoval {
        val ready = try {
            ensureModel()
        } catch (e: Exception) {
            Log.w(TAG, "The segmentation model isn't available", e)
            false
        }
        if (!ready) return BackgroundRemoval.Unavailable
        return try {
            val result = segmenter.process(InputImage.fromBitmap(photo, 0)).await()
            result.foregroundBitmap?.let { BackgroundRemoval.Done(it) } ?: BackgroundRemoval.NothingFound
        } catch (e: Exception) {
            BackgroundRemoval.Failed(e)
        }
    }

    /** True once the model is on the phone, downloading it first if needed. */
    private suspend fun ensureModel(): Boolean {
        val installer = ModuleInstall.getClient(appContext)
        if (installer.areModulesAvailable(segmenter).await().areModulesAvailable()) return true

        val finished = CompletableDeferred<Boolean>()
        val listener = InstallStatusListener { update: ModuleInstallStatusUpdate ->
            when (update.installState) {
                InstallState.STATE_COMPLETED -> finished.complete(true)
                InstallState.STATE_FAILED, InstallState.STATE_CANCELED -> finished.complete(false)
                else -> Unit
            }
        }
        val request = ModuleInstallRequest.newBuilder().addApi(segmenter).setListener(listener).build()
        return try {
            val response = installer.installModules(request).await()
            if (response.areModulesAlreadyInstalled()) true else withTimeoutOrNull(DOWNLOAD_TIMEOUT_MS) { finished.await() } ?: false
        } finally {
            installer.unregisterListener(listener)
        }
    }

    private companion object {
        const val TAG = "MlKitBackgroundRemover"
        const val DOWNLOAD_TIMEOUT_MS = 120_000L
    }
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { continuation.resume(it) }
    addOnFailureListener { continuation.resumeWithException(it) }
    addOnCanceledListener { continuation.cancel() }
}
