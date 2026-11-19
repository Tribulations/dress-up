package com.dressup.feature.profiles

import com.dressup.core.data.backup.ExportResult
import com.dressup.core.data.backup.ImportResult
import com.dressup.core.data.backup.ProfileBackup
import com.dressup.core.data.profile.DeleteProfileResult
import com.dressup.core.data.profile.ProfileNameResult
import com.dressup.core.data.profile.ProfileRepository
import com.dressup.core.model.Profile
import com.dressup.core.model.ProfilesState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.OutputStream

@OptIn(ExperimentalCoroutinesApi::class)
class WelcomeViewModelTest {
    private val anna = Profile("abc123", "Anna", isTestUser = false)
    private val profiles = FirstStartProfiles()
    private val backup = FakeBackup()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `an import on the first start makes the imported profile the active one`() = runTest {
        backup.result = ImportResult.Imported(anna, garments = 32, outfits = 3, diaryEntries = 15)
        profiles.state.update { it.copy(profiles = listOf(anna)) } // the import adds it, not yet active
        val vm = WelcomeViewModel(profiles, backup)
        vm.import { ByteArrayInputStream(ByteArray(0)) }
        assertEquals(anna.id, profiles.state.value.activeId)
        assertFalse(vm.uiState.value.importing)
        assertNull(vm.uiState.value.importError)
    }

    @Test
    fun `a failed import stays on the welcome screen and says why`() = runTest {
        val vm = WelcomeViewModel(profiles, backup)
        backup.result = ImportResult.NotAnExport
        vm.import { ByteArrayInputStream(ByteArray(0)) }
        assertEquals(WelcomeImportError.NOT_AN_EXPORT, vm.uiState.value.importError)
        vm.import { null }
        assertEquals(WelcomeImportError.FAILED, vm.uiState.value.importError)
        assertNull(profiles.state.value.activeId)
    }

    @Test
    fun `a blank name is refused`() {
        val vm = WelcomeViewModel(profiles, backup)
        vm.submit("  ")
        assertEquals(NameError.BLANK, vm.uiState.value.nameError)
    }

    private class FakeBackup : ProfileBackup {
        var result: ImportResult = ImportResult.Failed
        override suspend fun export(profileId: String, profileName: String, out: OutputStream) = ExportResult.Failed
        override suspend fun import(input: InputStream) = result
    }

    private class FirstStartProfiles : ProfileRepository {
        override val state = MutableStateFlow(ProfilesState())
        override fun createFirst(name: String): ProfileNameResult =
            if (name.isBlank()) ProfileNameResult.Blank else ProfileNameResult.Saved(Profile("main", name, false))
        override fun add(name: String) = createFirst(name)
        override fun rename(id: String, name: String) = createFirst(name)
        override fun switchTo(id: String) = state.update { it.copy(activeId = id) }
        override suspend fun delete(id: String) = DeleteProfileResult.DELETED
        override suspend fun openTestUser() = Unit
        override suspend fun resetTestUser() = Unit
    }
}
