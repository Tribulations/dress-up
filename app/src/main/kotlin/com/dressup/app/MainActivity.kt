package com.dressup.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.dressup.core.ui.DressUpTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val data = (application as DressUpApplication).data
        // Only on a fresh start: after the system restores us (e.g. during a camera trip) an open editor may hold a
        // photo that isn't saved yet.
        if (savedInstanceState == null) lifecycleScope.launch { data.cleanUpUnusedPhotos() }
        setContent {
            DressUpTheme { DressUpNavHost(data) }
        }
    }
}
