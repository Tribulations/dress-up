package com.dressup.feature.profiles

import com.dressup.core.data.profile.DeleteProfileResult
import com.dressup.core.data.profile.ProfileNameResult
import com.dressup.core.data.profile.ProfileRepository
import com.dressup.core.model.Profile
import com.dressup.core.model.ProfilesState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfilesViewModelTest {
    private val repo = FakeProfiles()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `tapping another own profile switches to it`() = runTest {
        repo.state.value = ProfilesState(listOf(ALEX, SAM), activeId = ALEX.id)
        ProfilesViewModel(repo).select(SAM)
        assertEquals(SAM.id, repo.state.value.activeId)
    }

    @Test
    fun `the test user is prepared once, with a busy state meanwhile, then switched to`() = runTest {
        val vm = ProfilesViewModel(repo)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        val gate = CompletableDeferred<Unit>()
        repo.testUserGate = gate

        vm.openTestUser()
        vm.openTestUser() // a second tap while preparing does nothing
        assertEquals(ProfilesBusy.PREPARING_TEST_USER, vm.uiState.value.busy)

        gate.complete(Unit)
        assertNull(vm.uiState.value.busy)
        assertEquals(1, repo.openTestUserCalls)
        assertEquals(TEST.id, repo.state.value.activeId)
    }

    @Test
    fun `a name problem keeps the dialog open with an error`() = runTest {
        val vm = ProfilesViewModel(repo)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        vm.startAdd()
        vm.submitName(" ")
        assertEquals(NameError.BLANK, vm.uiState.value.dialog?.error)

        vm.submitName("Sam")
        assertNull(vm.uiState.value.dialog)
        assertEquals(listOf("Sam"), repo.added)
    }

    @Test
    fun `deleting asks first, and a refused delete says why`() = runTest {
        repo.deleteResult = DeleteProfileResult.LAST_OWN
        val vm = ProfilesViewModel(repo)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        vm.askDelete(ALEX)
        assertEquals(ALEX, vm.uiState.value.confirmDelete)
        assertEquals(emptyList<String>(), repo.deleted)

        vm.confirmDelete()
        assertEquals(listOf(ALEX.id), repo.deleted)
        assertEquals(ProfilesMessage.CANT_DELETE_LAST, vm.uiState.value.message)
    }

    @Test
    fun `resetting the test user asks first and confirms when done`() = runTest {
        val vm = ProfilesViewModel(repo)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        vm.askReset()
        assertEquals(0, repo.resetCalls)

        vm.confirmReset()
        assertEquals(1, repo.resetCalls)
        assertEquals(ProfilesMessage.TEST_USER_RESET, vm.uiState.value.message)
    }

    private companion object {
        val ALEX = Profile("main", "Alex", isTestUser = false)
        val SAM = Profile("sam", "Sam", isTestUser = false)
        val TEST = Profile("test", "", isTestUser = true)
    }

    private class FakeProfiles : ProfileRepository {
        override val state = MutableStateFlow(ProfilesState(listOf(ALEX), activeId = ALEX.id))
        val added = mutableListOf<String>()
        val deleted = mutableListOf<String>()
        var deleteResult = DeleteProfileResult.DELETED
        var testUserGate: CompletableDeferred<Unit>? = null
        var openTestUserCalls = 0
        var resetCalls = 0

        override fun createFirst(name: String) = add(name)

        override fun add(name: String): ProfileNameResult {
            if (name.isBlank()) return ProfileNameResult.Blank
            added += name
            return ProfileNameResult.Saved(Profile(name.lowercase(), name, isTestUser = false))
        }

        override fun rename(id: String, name: String) = add(name)

        override fun switchTo(id: String) = state.update { it.copy(activeId = id) }

        override suspend fun delete(id: String): DeleteProfileResult {
            deleted += id
            return deleteResult
        }

        override suspend fun openTestUser() {
            openTestUserCalls++
            testUserGate?.await()
            state.update { it.copy(profiles = it.profiles + TEST, activeId = TEST.id) }
        }

        override suspend fun resetTestUser() {
            resetCalls++
        }
    }
}
