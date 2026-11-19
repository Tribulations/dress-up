package com.dressup.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dressup.core.data.profile.DressUpProfiles
import com.dressup.core.ui.DressUpTheme
import com.dressup.feature.profiles.WelcomeRoute

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val profiles = (application as DressUpApplication).profiles
        val appVersion = packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()
        val freshStart = savedInstanceState == null
        setContent {
            DressUpTheme { DressUpApp(profiles, appVersion, freshStart) }
        }
    }
}

/** "What's your name?" until there is a profile, then the active profile's wardrobe. */
@Composable
private fun DressUpApp(profiles: DressUpProfiles, appVersion: String, freshStart: Boolean) {
    val state by profiles.state.collectAsStateWithLifecycle()
    val active = state.active
    val firstProfileId = remember { state.activeId }

    ProfileScope(active?.id ?: NO_PROFILE) {
        if (active == null) {
            WelcomeRoute(profiles)
        } else {
            val data = profiles.data(active.id)
            // Only when no editor can hold an unsaved photo: on a fresh start, or just after switching profile. Not when
            // Android restores the app (e.g. during a camera trip).
            LaunchedEffect(active.id) {
                if (freshStart || active.id != firstProfileId) data.cleanUpUnusedPhotos()
            }
            DressUpNavHost(profiles = profiles, backup = profiles, data = data, profile = active, appVersion = appVersion)
        }
    }
}

private const val NO_PROFILE = ""
