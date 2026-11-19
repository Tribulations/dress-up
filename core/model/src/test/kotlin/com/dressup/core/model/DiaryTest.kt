package com.dressup.core.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DiaryTest {
    private fun entry(id: Long, date: String, outfit: Long? = null, garments: List<Long> = emptyList(), logged: Long = 0) =
        DiaryEntry(id, LocalDate.parse(date), outfit, garments, occasion = null, note = null, createdAtMillis = logged)

    private val diary = listOf(
        entry(1, "2026-11-01", garments = listOf(10)),
        entry(2, "2026-11-18", outfit = 5, garments = listOf(10, 11)),
        entry(3, "2025-12-24", outfit = 5, garments = listOf(11)),
        entry(4, "2026-11-18", garments = listOf(12), logged = 5),
    ).sortedWith(DIARY_ORDER)

    @Test
    fun `newest first, and on the same day the most recently logged first`() {
        assertEquals(listOf(4L, 2L, 1L, 3L), diary.map { it.id })
    }

    @Test
    fun `a garment's wears include the outfits it was worn in`() {
        assertEquals(listOf(2L, 1L), diary.wearsOfGarment(10).map { it.id })
        assertEquals(listOf(2L, 3L), diary.wearsOfOutfit(5).map { it.id })
    }

    @Test
    fun `the summary counts the wears and knows the last one`() {
        assertEquals(WearSummary(2, LocalDate.parse("2026-11-18")), diary.wearsOfOutfit(5).summary())
        assertEquals(WearSummary.NEVER, diary.wearsOfGarment(99).summary())
    }

    @Test
    fun `garment wears are counted for the whole wardrobe at once`() {
        val wears = diary.garmentWears()
        assertEquals(WearSummary(2, LocalDate.parse("2026-11-18")), wears[10])
        assertEquals(WearSummary(2, LocalDate.parse("2026-11-18")), wears[11])
        assertEquals(WearSummary(1, LocalDate.parse("2026-11-18")), wears[12])
        assertEquals(null, wears[99])
    }
}
