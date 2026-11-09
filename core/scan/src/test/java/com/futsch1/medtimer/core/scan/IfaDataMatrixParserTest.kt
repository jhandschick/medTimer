package com.futsch1.medtimer.core.scan

import org.junit.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull

class IfaDataMatrixParserTest {
    private val complete = PackageCode("12345678", "ABC123", LocalDate.of(2026, 12, 31), "1234567890")

    @Test
    fun `should read all fields when wrapped in macro 06`() {
        val raw = ifa("9N111234567842", "1TABC123", "D261231", "S1234567890")

        assertEquals(complete, IfaDataMatrixParser.parse(raw))
    }

    @Test
    fun `should read fields when macro header missing`() {
        val raw = ifa("9N111234567842", "1TABC123", "D261231", "S1234567890", macro = false)

        assertEquals(complete, IfaDataMatrixParser.parse(raw))
    }

    @Test
    fun `should read fields in any order and skip unknown identifiers`() {
        val raw = ifa("S1234567890", "8P04150123456782", "D261231", "9N111234567842", "1TABC123")

        assertEquals(complete, IfaDataMatrixParser.parse(raw))
    }

    @Test
    fun `should leave optional fields null when absent`() {
        assertEquals(PackageCode("12345678", null, null, null), IfaDataMatrixParser.parse(ifa("9N111234567842")))
    }

    @Test
    fun `should use last day of month for day 00`() {
        val code = IfaDataMatrixParser.parse(ifa("9N111234567842", "D260200"))

        assertEquals(LocalDate.of(2026, 2, 28), code?.expiry)
    }

    @Test
    fun `should return null when ppn check digits invalid`() {
        assertNull(IfaDataMatrixParser.parse(ifa("9N111234567843")))
    }

    @Test
    fun `should return null when embedded pzn invalid`() {
        assertNull(IfaDataMatrixParser.parse(ifa("9N110123456779")))
    }

    @Test
    fun `should return null when ppn does not start with 11`() {
        assertNull(IfaDataMatrixParser.parse(ifa("9N121234567842")))
    }

    @Test
    fun `should return null when ppn missing`() {
        assertNull(IfaDataMatrixParser.parse(ifa("1TABC123", "D261231")))
    }

    @Test
    fun `should return null when expiry invalid`() {
        assertNull(IfaDataMatrixParser.parse(ifa("9N111234567842", "D261300")))
    }

    private fun ifa(vararg fields: String, macro: Boolean = true): String {
        val body = fields.joinToString(GS)
        return if (macro) "[)>${RS}06$GS$body$RS$EOT" else body
    }

    private companion object {
        const val GS = "\u001D"
        const val RS = "\u001E"
        const val EOT = "\u0004"
    }
}
