package com.dressup.app

import android.content.Context
import com.dressup.adapter.cutout.MlKitBackgroundRemover
import com.dressup.core.data.photo.BackgroundRemover

/** Built with dressup.cutout=true: cut-outs with ML Kit (ARCHITECTURE.md §7.5). */
internal fun backgroundRemover(context: Context): BackgroundRemover = MlKitBackgroundRemover(context)
