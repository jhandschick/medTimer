package com.futsch1.medtimer.core.scan

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Code39PznParserTest {
    @Test
    fun `should read pzn when prefixed with dash`() {
        assertEquals(PackageCode("12345678", null, null, null), Code39PznParser.parse("-12345678"))
    }

    @Test
    fun `should read pzn when prefixed with PZN dash`() {
        assertEquals(PackageCode("01234562", null, null, null), Code39PznParser.parse("PZN-01234562"))
    }

    @Test
    fun `should read pzn when surrounded by whitespace`() {
        assertEquals(PackageCode("12345678", null, null, null), Code39PznParser.parse("  -12345678\n"))
    }

    @Test
    fun `should return null when check digit invalid`() {
        val results = listOf("-01234567", "-10000060").map { Code39PznParser.parse(it) }

        assertTrue(results.all { it == null }, "results: $results")
    }

    @Test
    fun `should return null when format differs`() {
        val results = listOf("12345678", "-1234567", "PZN12345678", "-1234567X").map { Code39PznParser.parse(it) }

        assertTrue(results.all { it == null }, "results: $results")
    }
}
