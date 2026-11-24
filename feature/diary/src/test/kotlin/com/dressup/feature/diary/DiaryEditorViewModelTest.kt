package com.dressup.feature.diary

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.dressup.core.data.CategoryNameResult
import com.dressup.core.data.CategoryRepository
import com.dressup.core.data.CutoutResult
import com.dressup.core.data.DeleteCategoryResult
import com.dressup.core.data.DiaryRepository
import com.dressup.core.data.GarmentRepository
import com.dressup.core.data.OutfitRepository
import com.dressup.core.data.photo.PhotoStorage
import com.dressup.core.model.Category
import com.dressup.core.model.DiaryDraft
import com.dressup.core.model.DiaryEntry
import com.dressup.core.model.Garment
import com.dressup.core.model.GarmentAspect
import com.dressup.core.model.GarmentDraft
import com.dressup.core.model.Outfit
import com.dressup.core.model.OutfitAspect
import com.dressup.core.model.OutfitDraft
import com.dressup.core.model.OutfitItem
import com.dressup.core.model.RatingChange
import com.dressup.core.model.Ratings
import com.dressup.core.ui.BOARD_ASPECT_RATIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class DiaryEditorViewModelTest {
    private val today = LocalDate.of(2026, 11, 19)
    private val diary = FakeDiary()
    private val outfits = FakeOutfits(
        listOf(Outfit(5, "Office", listOf(OutfitItem(1, 0.5f, 0.3f, 0.4f), OutfitItem(2, 0.5f, 0.7f, 0.4f)), createdAtMillis = 0)),
    )

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.editor(
        entryId: Long? = null,
        outfitId: Long? = null,
        garmentId: Long? = null,
        saved: SavedStateHandle = SavedStateHandle(),
    ): DiaryEditorViewModel {
        val vm = DiaryEditorViewModel(
            entryId, outfitId, garmentId, saved, diary, FakeGarments, FakeCategories, outfits, FakePhotos, today = { today },
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        return vm
    }

    @Test
    fun `logging from an outfit's page wears the outfit and its garments, today`() = runTest {
        val vm = editor(outfitId = 5)
        val state = vm.uiState.value
        assertEquals(5L, state.outfitId)
        assertEquals(listOf(1L, 2L), state.garmentIds)
        assertEquals(today, state.date)
        assertEquals("Office", state.worn?.outfitName)

        vm.setOccasion("Christmas Eve")
        vm.save()
        assertEquals(DiaryDraft(null, today, 5, listOf(1, 2), "Christmas Eve", ""), diary.saved.single())
        assertTrue(vm.uiState.value.done)
    }

    @Test
    fun `picking single garments replaces the outfit`() = runTest {
        val vm = editor(outfitId = 5)
        vm.pickGarments(listOf(3L))
        assertNull(vm.uiState.value.outfitId)
        assertEquals(listOf(3L), vm.uiState.value.garmentIds)
        assertEquals(listOf("Jeans"), vm.uiState.value.worn?.garmentTitles)
    }

    @Test
    fun `nothing can be saved until something is picked`() = runTest {
        val vm = editor()
        assertFalse(vm.uiState.value.canSave)
        vm.save()
        assertTrue(diary.saved.isEmpty())
        vm.chooseOutfit(5)
        assertTrue(vm.uiState.value.canSave)
    }

    @Test
    fun `the date can't be in the future`() = runTest {
        val vm = editor(garmentId = 1)
        vm.setDate(today.plusDays(3))
        assertEquals(today, vm.uiState.value.date)
        vm.setDate(LocalDate.of(2025, 12, 24))
        assertEquals(LocalDate.of(2025, 12, 24), vm.uiState.value.date)
    }

    @Test
    fun `editing keeps what was worn, even if the outfit changed since`() = runTest {
        diary.items.value = listOf(DiaryEntry(9, LocalDate.of(2025, 12, 24), 5, listOf(1), "Christmas Eve", "Warm", 0))
        val vm = editor(entryId = 9)
        assertEquals(listOf(1L), vm.uiState.value.garmentIds)
        vm.setNote("Too warm")
        vm.save()
        assertEquals(DiaryDraft(9, LocalDate.of(2025, 12, 24), 5, listOf(1), "Christmas Eve", "Too warm"), diary.saved.single())
    }

    @Test
    fun `the inputs survive the app being stopped`() = runTest {
        val saved = SavedStateHandle()
        val first = editor(garmentId = 1, saved = saved)
        first.setOccasion("Sam's birthday")
        first.setDate(LocalDate.of(2026, 11, 1))
        val restored = editor(garmentId = 1, saved = saved)
        assertEquals("Sam's birthday", restored.uiState.value.occasion)
        assertEquals(LocalDate.of(2026, 11, 1), restored.uiState.value.date)
        assertEquals(listOf(1L), restored.uiState.value.garmentIds)
    }

    @Test
    fun `occasions used before are suggested while typing`() {
        val used = listOf("Christmas Eve", "Sam's birthday", "Office party", "Birthday dinner", "Midsummer", "Cabin weekend")
        assertEquals(used.take(5), suggestions(used, ""))
        assertEquals(listOf("Sam's birthday", "Birthday dinner"), suggestions(used, "birth"))
        assertEquals(emptyList<String>(), suggestions(used, "christmas eve"))
    }

    @Test
    fun `the list groups entries by month, newest first`() {
        fun row(id: Long, date: String) = DiaryRow(
            DiaryEntry(id, LocalDate.parse(date), null, listOf(1), null, null, 0),
            WornThing(null, false, listOf("Tee"), emptyList(), null, false),
        )
        val months = byMonth(listOf(row(3, "2026-11-18"), row(2, "2026-11-02"), row(1, "2025-12-24")))
        assertEquals(listOf(YearMonth.of(2026, 11), YearMonth.of(2025, 12)), months.map { it.month })
        assertEquals(listOf(3L, 2L), months.first().rows.map { it.entry.id })
    }

    @Test
    fun `single garments fill the small collage, all on the board`() {
        listOf(1, 2, 3, 4, 6).forEach { count ->
            val layout = collageLayout((1L..count).toList())
            assertEquals(count, layout.size)
            layout.forEach { item ->
                val half = item.scale / 2
                assertTrue("$count: $item", item.x - half >= 0f && item.x + half <= 1f)
                val halfHeight = half * BOARD_ASPECT_RATIO // the board is 3:4, so a square piece is shorter in height fractions
                assertTrue("$count: $item", item.y - halfHeight >= 0f && item.y + halfHeight <= 1f)
            }
        }
    }

    private class FakeDiary : DiaryRepository {
        val items = MutableStateFlow<List<DiaryEntry>>(emptyList())
        val saved = mutableListOf<DiaryDraft>()
        override val entries: Flow<List<DiaryEntry>> = items
        override fun entry(id: Long): Flow<DiaryEntry?> = items.map { list -> list.firstOrNull { it.id == id } }
        override val occasions: Flow<List<String>> = MutableStateFlow(listOf("Christmas Eve"))
        override suspend fun save(draft: DiaryDraft): Long {
            saved += draft
            return draft.id ?: 1
        }
        override suspend fun delete(id: Long) = Unit
    }

    private class FakeOutfits(initial: List<Outfit>) : OutfitRepository {
        private val items = MutableStateFlow(initial)
        override val outfits: Flow<List<Outfit>> = items
        override fun outfit(id: Long): Flow<Outfit?> = items.map { list -> list.firstOrNull { it.id == id } }
        override suspend fun save(draft: OutfitDraft) = 0L
        override suspend fun rate(id: Long, change: RatingChange<OutfitAspect>) = Unit
        override suspend fun delete(id: Long) = Unit
    }

    private object FakeGarments : GarmentRepository {
        private fun garment(id: Long, name: String) = Garment(
            id, name, null, listOf(PANTS.id), emptyList(), "p$id.jpg", null, Ratings(), 0,
        )
        override val garments: Flow<List<Garment>> = MutableStateFlow(listOf(garment(1, "Oxford"), garment(2, "Chinos"), garment(3, "Jeans")))
        override fun garment(id: Long): Flow<Garment?> = garments.map { list -> list.firstOrNull { it.id == id } }
        override suspend fun save(draft: GarmentDraft) = 0L
        override suspend fun rate(id: Long, change: RatingChange<GarmentAspect>) = Unit
        override val canCutOut = false
        override suspend fun cutOut(id: Long) = CutoutResult.UNAVAILABLE
        override suspend fun removeCutout(id: Long) = Unit
        override suspend fun delete(id: Long) = Unit
    }

    private object FakeCategories : CategoryRepository {
        override val categories: Flow<List<Category>> = MutableStateFlow(listOf(PANTS))
        override suspend fun add(name: String) = CategoryNameResult.Blank
        override suspend fun rename(id: Long, name: String) = CategoryNameResult.Blank
        override suspend fun setHidden(id: Long, hidden: Boolean) = Unit
        override suspend fun delete(id: Long) = DeleteCategoryResult.DELETED
    }

    private object FakePhotos : PhotoStorage {
        override fun newCaptureUri(): Uri = error("not used")
        override suspend fun import(source: Uri): Result<String> = error("not used")
        override fun file(fileName: String) = File(fileName)
        override suspend fun delete(fileName: String) = Unit
        override suspend fun deleteAllExcept(keep: Set<String>) = Unit
    }

    private companion object {
        val PANTS = Category(id = 1, name = "Pants", builtIn = true, hidden = false)
    }
}
