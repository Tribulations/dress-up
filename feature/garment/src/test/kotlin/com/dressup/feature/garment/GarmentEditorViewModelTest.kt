package com.dressup.feature.garment

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.dressup.core.data.CategoryNameResult
import com.dressup.core.data.CategoryRepository
import com.dressup.core.data.DeleteCategoryResult
import com.dressup.core.data.GarmentRepository
import com.dressup.core.data.photo.PhotoStorage
import com.dressup.core.model.Category
import com.dressup.core.model.Garment
import com.dressup.core.model.GarmentColour
import com.dressup.core.model.GarmentDraft
import com.dressup.core.model.Rating
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class GarmentEditorViewModelTest {
    private val garments = FakeGarments()
    private val categories = FakeCategories()
    private val photos = FakePhotos()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun editor(id: Long? = null, saved: SavedStateHandle = SavedStateHandle()) =
        GarmentEditorViewModel(id, saved, garments, categories, photos)

    @Test
    fun `a new garment can only be saved once a category is chosen`() = runTest {
        val vm = editor()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        assertFalse(vm.uiState.value.canSave)

        vm.selectCategory(PANTS.id)
        assertTrue(vm.uiState.value.canSave)
    }

    @Test
    fun `saving a new garment saves category and colour, colour optional`() = runTest {
        val vm = editor()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        vm.selectCategory(PANTS.id)
        vm.selectColour(GarmentColour.NAVY)
        vm.save()

        assertEquals(GarmentDraft(null, PANTS.id, GarmentColour.NAVY, null), garments.savedDrafts.single())
        assertEquals(1L, vm.uiState.value.savedGarmentId)
    }

    @Test
    fun `editing loads the garment, and removing its photo never deletes the file before saving`() = runTest {
        garments.items.value = listOf(garment(id = 7, photo = "original.jpg"))
        val vm = editor(id = 7)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        assertEquals(PANTS.id, vm.uiState.value.selectedCategoryId)

        vm.removePhoto()
        assertTrue(photos.deleted.isEmpty())
    }

    @Test
    fun `hidden categories are not offered, unless the garment already uses one`() = runTest {
        categories.items.value = listOf(PANTS.copy(hidden = true), SHIRT)
        val newVm = editor()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { newVm.uiState.collect {} }
        assertEquals(listOf(SHIRT), newVm.uiState.value.categories)

        garments.items.value = listOf(garment(id = 7, photo = null))
        val editVm = editor(id = 7)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { editVm.uiState.collect {} }
        assertEquals(2, editVm.uiState.value.categories.size)
    }

    @Test
    fun `the inputs survive the app being stopped during a camera trip`() = runTest {
        val saved = SavedStateHandle()
        val first = editor(saved = saved)
        first.selectCategory(SHIRT.id)
        first.selectColour(GarmentColour.RED)

        val restored = editor(saved = saved)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { restored.uiState.collect {} }
        assertEquals(SHIRT.id, restored.uiState.value.selectedCategoryId)
        assertEquals(GarmentColour.RED, restored.uiState.value.colour)
    }

    @Test
    fun `a new category from the editor is selected`() = runTest {
        val vm = editor()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        vm.openCategoryDialog()
        vm.addCategory("Rain gear")

        assertEquals(categories.items.value.first { it.name == "Rain gear" }.id, vm.uiState.value.selectedCategoryId)
        assertEquals(null, vm.uiState.value.categoryDialog)
    }

    private fun garment(id: Long, photo: String?) = Garment(
        id = id, categoryId = PANTS.id, colour = null, photoFileName = photo, rating = null, liked = false, createdAtMillis = 0,
    )

    private companion object {
        val PANTS = Category(id = 1, name = "Pants", builtIn = true, hidden = false)
        val SHIRT = Category(id = 2, name = "Shirt", builtIn = true, hidden = false)
    }

    private class FakeGarments : GarmentRepository {
        val items = MutableStateFlow<List<Garment>>(emptyList())
        val savedDrafts = mutableListOf<GarmentDraft>()
        override val garments: Flow<List<Garment>> = items
        override fun garment(id: Long): Flow<Garment?> = items.map { list -> list.firstOrNull { it.id == id } }
        override suspend fun save(draft: GarmentDraft): Long {
            savedDrafts += draft
            return draft.id ?: savedDrafts.size.toLong()
        }
        override suspend fun setRating(id: Long, rating: Rating?) = Unit
        override suspend fun setLiked(id: Long, liked: Boolean) = Unit
        override suspend fun delete(id: Long) = Unit
    }

    private class FakeCategories : CategoryRepository {
        val items = MutableStateFlow(listOf(PANTS, SHIRT))
        override val categories: Flow<List<Category>> = items
        override suspend fun add(name: String): CategoryNameResult {
            val id = items.first().maxOf { it.id } + 1
            items.value = items.value + Category(id, name, builtIn = false, hidden = false)
            return CategoryNameResult.Saved(id)
        }
        override suspend fun rename(id: Long, name: String) = CategoryNameResult.Saved(id)
        override suspend fun setHidden(id: Long, hidden: Boolean) = Unit
        override suspend fun delete(id: Long) = DeleteCategoryResult.DELETED
    }

    private class FakePhotos : PhotoStorage {
        val deleted = mutableListOf<String>()
        override fun newCaptureUri(): Uri = error("not used in these tests")
        override suspend fun import(source: Uri): Result<String> = Result.success("new.jpg")
        override fun file(fileName: String) = File(fileName)
        override suspend fun delete(fileName: String) { deleted += fileName }
        override suspend fun deleteAllExcept(keep: Set<String>) = Unit
    }
}
