package com.dressup.app

import android.app.Application
import com.dressup.core.data.profile.DressUpProfiles

class DressUpApplication : Application() {
    /** The profiles and each profile's data layer, for the whole app (manual dependency injection, ARCHITECTURE.md §2). */
    val profiles: DressUpProfiles by lazy { DressUpProfiles(this, backgroundRemover(this), appVersion = packageManager.getPackageInfo(packageName, 0).versionName) }
}
