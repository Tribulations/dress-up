package com.dressup.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Runs [content] (the whole navigation graph) for one profile. On a profile switch everything inside is rebuilt: a new
 * back stack, and the old profile's ViewModels are cleared, so no screen keeps showing the previous wardrobe
 * (ARCHITECTURE.md §7.4). The store lives in an activity ViewModel, so it survives rotation.
 */
@Composable
fun ProfileScope(profileId: String, content: @Composable () -> Unit) {
    val stores: ProfileViewModelStores = viewModel()
    val owner = remember(profileId) {
        val store = stores.storeFor(profileId)
        object : ViewModelStoreOwner {
            override val viewModelStore = store
        }
    }
    key(profileId) {
        CompositionLocalProvider(LocalViewModelStoreOwner provides owner, content = content)
    }
}

/** Holds the active profile's [ViewModelStore], and clears the previous one on a switch. */
class ProfileViewModelStores : ViewModel() {
    private var profileId: String? = null
    private var store = ViewModelStore()

    fun storeFor(id: String): ViewModelStore {
        if (id != profileId) {
            store.clear()
            store = ViewModelStore()
            profileId = id
        }
        return store
    }

    override fun onCleared() = store.clear()
}
