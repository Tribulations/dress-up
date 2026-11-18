package com.dressup.app

import android.app.Application
import com.dressup.core.data.DressUpData

class DressUpApplication : Application() {
    /** The one data layer for the whole app (manual dependency injection, ARCHITECTURE.md §2). */
    val data: DressUpData by lazy { DressUpData(this) }
}
