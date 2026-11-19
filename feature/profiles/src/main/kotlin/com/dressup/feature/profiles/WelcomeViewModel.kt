package com.dressup.feature.profiles

import androidx.lifecycle.ViewModel
import com.dressup.core.data.profile.ProfileNameResult
import com.dressup.core.data.profile.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The first start: asks the user's name and makes their profile. The app then shows that profile's wardrobe. */
class WelcomeViewModel(private val profiles: ProfileRepository) : ViewModel() {
    private val mutableError = MutableStateFlow<NameError?>(null)
    val error: StateFlow<NameError?> = mutableError.asStateFlow()

    fun submit(name: String) {
        mutableError.value = when (profiles.createFirst(name)) {
            is ProfileNameResult.Saved -> null
            ProfileNameResult.Blank -> NameError.BLANK
            ProfileNameResult.Duplicate -> NameError.DUPLICATE
        }
    }
}
