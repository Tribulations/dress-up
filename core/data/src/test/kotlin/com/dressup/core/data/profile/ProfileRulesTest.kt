package com.dressup.core.data.profile

import com.dressup.core.model.Profile
import com.dressup.core.model.ProfilesState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileRulesTest {
    private val main = Profile(MAIN_PROFILE_ID, "Alex", isTestUser = false)
    private val second = Profile("abc", "Sam", isTestUser = false)
    private val test = Profile(TEST_USER_PROFILE_ID, "", isTestUser = true)

    @Test
    fun `the first profile keeps the v0_1_0 file names, so updating loses nothing`() {
        assertEquals(ProfilePaths("dress-up.db", "photos", "photos"), ProfilePaths.of(MAIN_PROFILE_ID))
    }

    @Test
    fun `every other profile has its own database and photo folder`() {
        assertEquals(ProfilePaths("dress-up-test.db", "profiles/test/photos", "profiles/test"), ProfilePaths.of(TEST_USER_PROFILE_ID))
        assertEquals(ProfilePaths("dress-up-abc.db", "profiles/abc/photos", "profiles/abc"), ProfilePaths.of("abc"))
    }

    @Test
    fun `names are trimmed, may not be blank and are unique ignoring case`() {
        val state = ProfilesState(listOf(main, test), activeId = MAIN_PROFILE_ID)
        assertEquals("Sam Lee", cleanProfileName("  Sam   Lee "))
        assertEquals(ProfileNameResult.Blank, state.nameProblem(cleanProfileName("   ")))
        assertEquals(ProfileNameResult.Duplicate, state.nameProblem("alex"))
        assertNull(state.nameProblem("alex", exceptId = MAIN_PROFILE_ID))
        assertNull(state.nameProblem("Sam"))
    }

    @Test
    fun `the active profile can't be deleted`() {
        val state = ProfilesState(listOf(main, second), activeId = MAIN_PROFILE_ID)
        assertEquals(DeleteProfileResult.ACTIVE, state.deleteProblem(MAIN_PROFILE_ID))
        assertNull(state.deleteProblem("abc"))
    }

    @Test
    fun `the last own profile can't be deleted, the test user can`() {
        val state = ProfilesState(listOf(main, test), activeId = TEST_USER_PROFILE_ID)
        assertEquals(DeleteProfileResult.LAST_OWN, state.deleteProblem(MAIN_PROFILE_ID))
        assertNull(ProfilesState(listOf(main, test), activeId = MAIN_PROFILE_ID).deleteProblem(TEST_USER_PROFILE_ID))
    }
}
