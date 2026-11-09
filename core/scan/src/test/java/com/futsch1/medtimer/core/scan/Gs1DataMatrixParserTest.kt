package com.futsch1.medtimer.core.scan

import org.junit.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull

class Gs1DataMatrixParserTest {
    private val complete = PackageCode("12345678", "ABC123", LocalDate.of(2026, 12, 31), "SN0001")
    private val completeBody = "0104150123456782" + "17261231" + "10ABC123$GS" + "21SN0001"

    @Test
    fun `should read all fields`() {
        assertEquals(complete, Gs1DataMatrixParser.parse(gs1(completeBody)))
    }

    @Test
    fun `should parse identically with leading GS or without prefix`() {
        val results = listOf(GS, "").map { Gs1DataMatrixParser.parse(gs1(completeBody, prefix = it)) }

        assertEquals(listOf(complete, complete), results)
    }

    @Test
    fun `should read variable fields in any order`() {
        val raw = gs1("0104150123456782" + "21SN0001$GS" + "17261231" + "10ABC123")

        assertEquals(complete, Gs1DataMatrixParser.parse(raw))
    }

    @Test
    fun `should skip unknown fixed and variable ais`() {
        val raw = gs1("0104150123456782" + "11250101" + "17261231" + "240XYZ$GS" + "10ABC123$GS" + "21SN0001")

        assertEquals(complete, Gs1DataMatrixParser.parse(raw))
    }

    @Test
    fun `should use last day of month for day 00`() {
        val code = Gs1DataMatrixParser.parse(gs1("0104150123456782" + "17260200"))

        assertEquals(LocalDate.of(2026, 2, 28), code?.expiry)
    }

    @Test
    fun `should take pzn from ai 710 when gtin is international`() {
        val code = Gs1DataMatrixParser.parse(gs1("0104006381333931" + "71012345678$GS" + "17261231"))

        assertEquals("12345678", code?.pzn)
    }

    @Test
    fun `should return null when gtin international and no 710`() {
        assertNull(Gs1DataMatrixParser.parse(gs1("0104006381333931" + "17261231")))
    }

    @Test
    fun `should return null when gtin check digit invalid`() {
        assertNull(Gs1DataMatrixParser.parse(gs1("0104150123456783" + "17261231")))
    }

    @Test
    fun `should return null when embedded pzn invalid`() {
        assertNull(Gs1DataMatrixParser.parse(gs1("0104150012345678" + "17261231")))
    }

    @Test
    fun `should return null when expiry invalid`() {
        assertNull(Gs1DataMatrixParser.parse(gs1("0104150123456782" + "17261300")))
    }

    private fun gs1(body: String, prefix: String = "]d2") = prefix + body

    private companion object {
        const val GS = "\u001D"
    }
}
