package com.dressup.core.ui

import androidx.lifecycle.SavedStateHandle
import com.dressup.core.model.BoardColour
import com.dressup.core.model.OutfitBackground
import com.dressup.core.model.OutfitItem
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Test

class SavedDraftTest {
    @Serializable
    private data class Draft(val name: String = "", val items: List<OutfitItem> = emptyList(), val background: OutfitBackground? = null)

    @Test
    fun `a draft comes back whole in a new editor on the same saved state`() {
        val saved = SavedStateHandle()
        val first = SavedDraft(saved, Draft.serializer()) { Draft() }
        first.update { it.copy(name = "Sunday", items = listOf(OutfitItem(3, 0.2f, 0.4f, 0.5f, rotation = -20f, locked = true))) }
        first.update { it.copy(background = OutfitBackground.Colour(BoardColour.SAGE)) }

        val restored = SavedDraft(saved, Draft.serializer()) { Draft(name = "unused") }
        assertEquals(first.value, restored.value)
        assertEquals(first.value, restored.flow.value)
    }

    @Test
    fun `nothing saved, or a draft that no longer reads, starts from the initial value`() {
        assertEquals(Draft("new"), SavedDraft(SavedStateHandle(), Draft.serializer()) { Draft("new") }.value)
        val broken = SavedStateHandle(mapOf("draft" to "{not json"))
        assertEquals(Draft("new"), SavedDraft(broken, Draft.serializer()) { Draft("new") }.value)
    }
}
