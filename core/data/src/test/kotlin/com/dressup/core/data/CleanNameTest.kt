package com.dressup.core.data

import org.junit.Assert.assertEquals
import org.junit.Test

class CleanNameTest {
    @Test
    fun `whitespace is trimmed and collapsed`() {
        assertEquals("Rain gear", cleanName("  Rain \t  gear "))
    }

    @Test
    fun `long names are cut to the maximum length`() {
        assertEquals(MAX_CATEGORY_NAME_LENGTH, cleanName("x".repeat(100)).length)
    }
}
