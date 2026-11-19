package com.dressup.core.model

import java.time.LocalDate

/**
 * One wear in the diary (ARCHITECTURE.md §5.3): on [date], the user wore [outfitId] and/or [garmentIds], maybe for an
 * [occasion] ("Christmas Eve"), with an optional [note].
 *
 * [garmentIds] is what was actually worn: logging an outfit copies its garments in, so editing the outfit later
 * doesn't rewrite history, and a deleted outfit leaves its garments' wears behind.
 */
data class DiaryEntry(
    val id: Long,
    val date: LocalDate,
    val outfitId: Long?,
    val garmentIds: List<Long>,
    val occasion: String?,
    val note: String?,
    val createdAtMillis: Long,
)

/** What the diary editor saves: a new entry when [id] is null, otherwise an update. */
data class DiaryDraft(
    val id: Long?,
    val date: LocalDate,
    val outfitId: Long?,
    val garmentIds: List<Long>,
    val occasion: String?,
    val note: String?,
)

const val MAX_OCCASION_LENGTH = 40
const val MAX_NOTE_LENGTH = 1000

/** How often something was worn, and when last. */
data class WearSummary(val count: Int, val lastWorn: LocalDate?) {
    companion object {
        val NEVER = WearSummary(0, null)
    }
}

/** The entries in which garment [garmentId] was worn, newest first (the diary's own order). */
fun List<DiaryEntry>.wearsOfGarment(garmentId: Long): List<DiaryEntry> = filter { garmentId in it.garmentIds }

/** The entries in which outfit [outfitId] was worn. */
fun List<DiaryEntry>.wearsOfOutfit(outfitId: Long): List<DiaryEntry> = filter { it.outfitId == outfitId }

/** How often and when last each garment was worn, in one pass over the diary. Never-worn garments are missing. */
fun List<DiaryEntry>.garmentWears(): Map<Long, WearSummary> {
    val wears = HashMap<Long, WearSummary>()
    forEach { entry ->
        entry.garmentIds.distinct().forEach { id ->
            val before = wears[id] ?: WearSummary.NEVER
            wears[id] = WearSummary(before.count + 1, before.lastWorn?.let { maxOf(it, entry.date) } ?: entry.date)
        }
    }
    return wears
}

fun List<DiaryEntry>.summary(): WearSummary = WearSummary(size, maxOfOrNull { it.date })

/** Newest first: by date, then the most recently logged first. */
val DIARY_ORDER: Comparator<DiaryEntry> = compareByDescending<DiaryEntry> { it.date }.thenByDescending { it.createdAtMillis }.thenByDescending { it.id }
