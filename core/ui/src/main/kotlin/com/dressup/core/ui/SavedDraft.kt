package com.dressup.core.ui

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

/**
 * An editor's inputs, kept in [SavedStateHandle] as one value (JSON), so nothing is lost if Android stops the app in
 * the background, e.g. during a trip to the camera. A field added to [T] (a `@Serializable` class) is saved with it;
 * a draft that no longer reads (never across an app update in practice) starts over from [initial].
 */
class SavedDraft<T>(
    private val savedState: SavedStateHandle,
    private val serializer: KSerializer<T>,
    initial: () -> T,
) {
    private val state = MutableStateFlow(restore() ?: initial())

    val flow: StateFlow<T> = state.asStateFlow()

    val value: T get() = state.value

    fun update(change: (T) -> T) {
        state.value = change(state.value)
        savedState[KEY] = json.encodeToString(serializer, state.value)
    }

    private fun restore(): T? =
        savedState.get<String>(KEY)?.let { runCatching { json.decodeFromString(serializer, it) }.getOrNull() }

    private companion object {
        const val KEY = "draft"
        val json = Json { ignoreUnknownKeys = true }
    }
}

/** How a ViewModel's state is shared with its screen: kept for a few seconds without one, e.g. while it rotates. */
val WhileUiSubscribed: SharingStarted = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000)
