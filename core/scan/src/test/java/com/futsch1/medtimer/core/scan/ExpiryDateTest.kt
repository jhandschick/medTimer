package com.futsch1.medtimer.core.scan

import org.junit.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExpiryDateTest {
    @Test
    fun `should parse date when all six digits are valid`() {
        assertEquals(LocalDate.of(2026, 12, 31), parseExpiry("261231"))
    }

    @Test
    fun `should use last day of month when day is 00`() {
        val dates = listOf("260200", "280200", "261100").map { parseExpiry(it) }

        assertEquals(listOf(LocalDate.of(2026, 2, 28), LocalDate.of(2028, 2, 29), LocalDate.of(2026, 11, 30)), dates)
    }

    @Test
    fun `should return null when month or day invalid`() {
        val dates = listOf("261300", "260231", "26123", "26AB31", "2612311").map { parseExpiry(it) }

        assertTrue(dates.all { it == null }, "dates: $dates")
    }
}
