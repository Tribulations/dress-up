package com.dressup.feature.outfits

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.dressup.core.data.CategoryNameResult
import com.dressup.core.data.CategoryRepository
import com.dressup.core.data.CutoutResult
import com.dressup.core.data.DeleteCategoryResult
import com.dressup.core.data.GarmentRepository
import com.dressup.core.data.OutfitRepository
import com.dressup.core.data.photo.PhotoStorage
import com.dressup.core.model.Category
import com.dressup.core.model.Garment
import com.dressup.core.model.GarmentAspect
import com.dressup.core.model.GarmentDraft
import com.dressup.core.model.Outfit
import com.dressup.core.model.BoardColour
import com.dressup.core.model.OutfitAspect
import com.dressup.core.model.OutfitBackground
import com.dressup.core.ui.BoardBackground
import com.dressup.core.model.OutfitDraft
import com.dressup.core.model.OutfitItem
import com.dressup.core.model.Rating
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

@OptIn(ExperimentalCoroutinesApi::class)
class OutfitEditorViewModelTest {
    private val outfits = FakeOutfits()
    private val garments = FakeGarments(listOf(garment(1, "Sweater", cutout = true), garment(2, null), garment(3, "Boots")))

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.editor(id: Long? = null, saved: SavedStateHandle = SavedStateHandle()): OutfitEditorViewModel {
        val vm = OutfitEditorViewModel(id, saved, outfits, garments, FakeCategories, FakePhotos)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        return vm
    }

    @Test
    fun `a new outfit starts in the gallery of every garment, and needs a garment to be saved`() = runTest {
        val vm = editor()
        val state = vm.uiState.value
        assertTrue(state.galleryOpen)
        assertEquals(listOf(1L, 2L, 3L), state.gallery.map { it.garmentId })
        assertEquals("Pants", state.gallery[1].title) // no name: its category
        assertTrue(state.gallery[0].isCutout)
        assertFalse(state.canSave)

        vm.pickGarments(listOf(3L, 1L))
        assertFalse(vm.uiState.value.galleryOpen)
        assertEquals(listOf(3L, 1L), vm.uiState.value.pieces.map { it.item.garmentId })
        assertTrue(vm.uiState.value.canSave)
    }

    @Test
    fun `saving stores the name and where every garment sits`() = runTest {
        val vm = editor()
        vm.pickGarments(listOf(1L, 3L))
        vm.setName("Sunday walk")
        vm.select(1)
        vm.transform(1, dx = 0.1f, dy = 0f, zoom = 1f)
        vm.save()

        val draft = outfits.saved.single()
        assertEquals("Sunday walk", draft.name)
        // Selecting garment 1 brought it to the front, i.e. last.
        assertEquals(listOf(3L, 1L), draft.items.map { it.garmentId })
        assertEquals(0.28f + 0.1f, draft.items[1].x, 0.0001f) // its start position, moved right by a tenth
        assertTrue(vm.uiState.value.done)
    }

    @Test
    fun `editing loads the outfit, and removing the selected garment takes it off the board`() = runTest {
        outfits.items.value = listOf(Outfit(7, "Office", listOf(OutfitItem(1, 0.5f, 0.3f, 0.4f), OutfitItem(3, 0.5f, 0.8f, 0.3f)), createdAtMillis = 0))
        val vm = editor(id = 7)
        assertFalse(vm.uiState.value.galleryOpen)
        assertEquals("Office", vm.uiState.value.name)

        vm.select(3)
        vm.removeSelected()
        assertEquals(listOf(1L), vm.uiState.value.pieces.map { it.item.garmentId })
        assertNull(vm.uiState.value.selectedGarmentId)
    }

    @Test
    fun `the board survives the app being stopped`() = runTest {
        val saved = SavedStateHandle()
        val first = editor(saved = saved)
        first.pickGarments(listOf(2L))
        first.setName("Lazy day")

        val restored = editor(saved = saved)
        assertEquals("Lazy day", restored.uiState.value.name)
        assertEquals(listOf(2L), restored.uiState.value.pieces.map { it.item.garmentId })
    }

    @Test
    fun `a garment deleted from the wardrobe disappears from the board`() = runTest {
        outfits.items.value = listOf(Outfit(7, null, listOf(OutfitItem(1, 0.5f, 0.3f, 0.4f), OutfitItem(99, 0.5f, 0.8f, 0.3f)), createdAtMillis = 0))
        val vm = editor(id = 7)
        assertEquals(listOf(1L), vm.uiState.value.pieces.map { it.item.garmentId })
    }

    @Test
    fun `turning and a background colour are saved, and survive the app being stopped`() = runTest {
        val saved = SavedStateHandle()
        val vm = editor(saved = saved)
        vm.pickGarments(listOf(1L, 3L))
        vm.transform(1, dx = 0f, dy = 0f, zoom = 1f, rotation = 200f) // a two-finger twist past half a turn
        vm.rotate(3, -15f)
        vm.setBackgroundColour(BoardColour.SAGE)
        assertEquals(BoardBackground.Colour(BoardColour.SAGE.argb), vm.uiState.value.boardBackground)

        val restored = editor(saved = saved)
        assertEquals(listOf(-160f, -15f), restored.uiState.value.pieces.map { it.item.rotation })
        assertEquals(OutfitBackground.Colour(BoardColour.SAGE), restored.uiState.value.background)

        restored.save()
        val draft = outfits.saved.single()
        assertEquals(OutfitBackground.Colour(BoardColour.SAGE), draft.background)
        assertEquals(listOf(-160f, -15f), draft.items.map { it.rotation })
    }

    @Test
    fun `editing keeps the outfit's background until it is changed`() = runTest {
        outfits.items.value = listOf(
            Outfit(7, "Office", listOf(OutfitItem(1, 0.5f, 0.3f, 0.4f, rotation = 10f)), background = OutfitBackground.Image("bg.jpg"), createdAtMillis = 0),
        )
        val vm = editor(id = 7)
        assertEquals(BoardBackground.Image(File("bg.jpg")), vm.uiState.value.boardBackground)
        vm.setBackgroundColour(null)
        vm.save()
        assertNull(outfits.saved.single().background)
        assertEquals(10f, outfits.saved.single().items.single().rotation)
    }

    private companion object {
        val PANTS = Category(id = 1, name = "Pants", builtIn = true, hidden = false)

        fun garment(id: Long, name: String?, cutout: Boolean = false) = Garment(
            id = id, name = name, description = null, categoryIds = listOf(PANTS.id), colour = null,
            photoFileName = "photo$id.jpg", cutoutFileName = if (cutout) "cutout$id.webp" else null, rating = null,
            aspectRatings = emptyMap(), review = null, liked = false, createdAtMillis = id,
        )
    }

    private class FakeOutfits : OutfitRepository {
        val items = MutableStateFlow<List<Outfit>>(emptyList())
        val saved = mutableListOf<OutfitDraft>()
        override val outfits: Flow<List<Outfit>> = items
        override fun outfit(id: Long): Flow<Outfit?> = items.map { list -> list.firstOrNull { it.id == id } }
        override suspend fun save(draft: OutfitDraft): Long {
            saved += draft
            return draft.id ?: saved.size.toLong()
        }
        override suspend fun setRating(id: Long, rating: Rating?) = Unit
        override suspend fun setAspectRating(id: Long, aspect: OutfitAspect, rating: Rating?) = Unit
        override suspend fun setReview(id: Long, review: String?) = Unit
        override suspend fun setLiked(id: Long, liked: Boolean) = Unit
        override suspend fun delete(id: Long) = Unit
    }

    private class FakeGarments(initial: List<Garment>) : GarmentRepository {
        override val garments: Flow<List<Garment>> = MutableStateFlow(initial)
        override fun garment(id: Long): Flow<Garment?> = garments.map { list -> list.firstOrNull { it.id == id } }
        override suspend fun save(draft: GarmentDraft) = 0L
        override suspend fun setRating(id: Long, rating: Rating?) = Unit
        override suspend fun setAspectRating(id: Long, aspect: GarmentAspect, rating: Rating?) = Unit
        override suspend fun setReview(id: Long, review: String?) = Unit
        override suspend fun setLiked(id: Long, liked: Boolean) = Unit
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
}
