package com.howmanyhours

import org.junit.Test
import org.junit.Assert.*

class SimpleTest {

    @Test
    fun `simple test should pass`() {
        // Given
        val expected = 42

        // When
        val actual = 40 + 2

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `string test should pass`() {
        // Given
        val projectName = "Test Project"

        // When
        val result = projectName.uppercase()

        // Then
        assertEquals("TEST PROJECT", result)
    }
}