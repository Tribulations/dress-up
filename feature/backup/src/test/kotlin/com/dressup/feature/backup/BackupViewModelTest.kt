package com.dressup.feature.backup

import com.dressup.core.data.backup.ExportResult
import com.dressup.core.data.backup.ImportResult
import com.dressup.core.data.backup.ProfileBackup
import com.dressup.core.model.Profile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class BackupViewModelTest {

    private class FakeBackup(var importResult: ImportResult = ImportResult.NotAnExport) : ProfileBackup {
        var exportedId: String? = null
        override suspend fun export(profileId: String, profileName: String, out: OutputStream): ExportResult {
            exportedId = profileId
            out.write(1)
            return ExportResult.Done(garments = 32, pictures = 50)
        }
        override suspend fun import(input: InputStream) = importResult
    }


    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun vm(backup: ProfileBackup) = BackupViewModel(backup, "main", "Anna", today = { LocalDate.of(2026, 11, 19) })

    private val output = { ByteArrayOutputStream() }
    private val input = { ByteArrayInputStream(ByteArray(0)) }

    @Test
    fun `export writes the active profile and says how much`() = runTest {
        val backup = FakeBackup()
        val vm = vm(backup)
        assertEquals("dress-up-anna-2026-11-19.zip", vm.suggestedFileName())
        vm.export(output)
        assertEquals("main", backup.exportedId)
        assertEquals(BackupMessage.Exported(32), vm.uiState.value.message)
        assertFalse(vm.uiState.value.working)
    }

    @Test
    fun `an import reports the new profile, or why it didn't work`() = runTest {
        val profile = Profile("abc", "Anna (2)", isTestUser = false)
        val backup = FakeBackup(ImportResult.Imported(profile, 3, 1, 4))
        val vm = vm(backup)
        vm.import(input)
        assertEquals(BackupMessage.Imported(profile, 3, 1, 4), vm.uiState.value.message)

        backup.importResult = ImportResult.TooNew
        vm.import(input)
        assertEquals(BackupMessage.TooNew, vm.uiState.value.message)
    }

    @Test
    fun `a file that can't be opened is a failure, not a crash`() = runTest {
        val vm = vm(FakeBackup())
        vm.export { null }
        assertEquals(BackupMessage.ExportFailed, vm.uiState.value.message)
        vm.import { throw java.io.FileNotFoundException("gone") }
        assertEquals(BackupMessage.ImportFailed, vm.uiState.value.message)
    }
}
