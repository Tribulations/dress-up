package com.dressup.feature.garment

import android.net.Uri
import com.dressup.core.data.CategoryNameResult
import com.dressup.core.data.CategoryRepository
import com.dressup.core.data.CutoutResult
import com.dressup.core.data.DeleteCategoryResult
import com.dressup.core.data.photo.PhotoStorage
import com.dressup.core.model.Category
import com.dressup.core.model.GarmentAspect
import com.dressup.core.model.Rating
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
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
import com.dressup.core.model.DiaryEntry
import com.dressup.core.model.WearSummary
import com.dressup.core.model.summary
import java.time.LocalDate
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class GarmentDetailViewModelTest {
    private val garments = FakeGarments()
    private val diary = FakeDiary()
    private val categories = object : CategoryRepository {
        override val categories: Flow<List<Category>> = MutableStateFlow(listOf(SWEATER, SPORTSWEAR))
        override suspend fun add(name: String) = CategoryNameResult.Blank
        override suspend fun rename(id: Long, name: String) = CategoryNameResult.Blank
        override suspend fun setHidden(id: Long, hidden: Boolean) = Unit
        override suspend fun delete(id: Long) = DeleteCategoryResult.DELETED
    }
    private val photos = object : PhotoStorage {
        override fun newCaptureUri(): Uri = error("not used")
        override suspend fun import(source: Uri): Result<String> = error("not used")
        override fun file(fileName: String) = File(fileName)
        override suspend fun delete(fileName: String) = Unit
        override suspend fun deleteAllExcept(keep: Set<String>) = Unit
    }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScopeHolder.loaded() = vm.uiState.value as GarmentDetailUiState.Loaded

    private class TestScopeHolder(val vm: GarmentDetailViewModel)

    private fun kotlinx.coroutines.test.TestScope.detail(): TestScopeHolder {
        val vm = GarmentDetailViewModel(1, garments, categories, photos, diary)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        return TestScopeHolder(vm)
    }

    @Test
    fun `the name is the title and all categories the subtitle, without a name the main category is the title`() = runTest {
        garments.items.value = listOf(testGarment(1, listOf(SWEATER.id, SPORTSWEAR.id)).copy(name = "Coola favvis tröjan"))
        val page = detail()
        assertEquals("Coola favvis tröjan", page.loaded().title)
        assertEquals("Sweater · Sportswear", page.loaded().subtitle)

        garments.items.value = listOf(testGarment(1, listOf(SWEATER.id, SPORTSWEAR.id)))
        assertEquals("Sweater", page.loaded().title)
    }

    @Test
    fun `aspect ratings and the review are saved and can be cleared`() = runTest {
        garments.items.value = listOf(testGarment(1, listOf(SWEATER.id)))
        val page = detail()
        page.vm.setAspectRating(GarmentAspect.FIT, Rating(8))
        page.vm.setReview("  Warm and soft  ")
        assertEquals(Rating(8), page.loaded().garment.aspectRatings[GarmentAspect.FIT])
        assertEquals("Warm and soft", page.loaded().garment.review)

        page.vm.setAspectRating(GarmentAspect.FIT, null)
        page.vm.setReview("   ")
        assertTrue(page.loaded().garment.aspectRatings.isEmpty())
        assertNull(page.loaded().garment.review)
    }

    @Test
    fun `a cut-out replaces the photo on the page, and the original can be brought back`() = runTest {
        garments.items.value = listOf(testGarment(1, listOf(SWEATER.id), photo = "photo.jpg"))
        val page = detail()
        assertTrue(page.loaded().canCutOut)
        assertEquals(File("photo.jpg"), page.loaded().picture)

        page.vm.cutOut()
        assertTrue(page.loaded().isCutout)
        assertEquals(File("cutout.webp"), page.loaded().picture)

        page.vm.useOriginalPhoto()
        assertFalse(page.loaded().isCutout)
        assertEquals(File("photo.jpg"), page.loaded().picture)
    }

    @Test
    fun `a failed cut-out says why, and there is nothing to cut out without a photo`() = runTest {
        garments.items.value = listOf(testGarment(1, listOf(SWEATER.id), photo = "photo.jpg"))
        garments.cutoutResult = CutoutResult.NOTHING_FOUND
        val page = detail()
        page.vm.cutOut()
        assertEquals(DetailMessage.NOTHING_FOUND, page.loaded().message)
        assertFalse(page.loaded().cuttingOut)
        page.vm.messageShown()
        assertNull(page.loaded().message)

        garments.items.value = listOf(testGarment(1, listOf(SWEATER.id), photo = null))
        assertFalse(page.loaded().canCutOut)
    }

    private companion object {
        val SWEATER = Category(id = 1, name = "Sweater", builtIn = true, hidden = false)
        val SPORTSWEAR = Category(id = 2, name = "Sportswear", builtIn = false, hidden = false)
    }

    @Test
    fun `the page lists the diary entries the garment was worn in, newest first`() = runTest {
        garments.items.value = listOf(testGarment(1, listOf(SWEATER.id)))
        diary.items.value = listOf(
            DiaryEntry(3, LocalDate.of(2026, 11, 18), outfitId = 5, garmentIds = listOf(1, 2), occasion = "Office", note = null, createdAtMillis = 0),
            DiaryEntry(2, LocalDate.of(2026, 11, 10), outfitId = null, garmentIds = listOf(2), occasion = null, note = null, createdAtMillis = 0),
            DiaryEntry(1, LocalDate.of(2025, 12, 24), outfitId = null, garmentIds = listOf(1), occasion = "Christmas Eve", note = null, createdAtMillis = 0),
        )
        val page = detail()
        assertEquals(listOf(3L, 1L), page.loaded().wears.map { it.id })
        assertEquals(WearSummary(2, LocalDate.of(2026, 11, 18)), page.loaded().wears.summary())
    }
}
