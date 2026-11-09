package com.futsch1.medtimer.core.scan

import org.junit.Test
import java.time.LocalDate
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PackageCodeParserTest {
    @Test
    fun `should dispatch each format`() {
        val inputs = listOf(
            "PZN-12345678",
            "[)>${RS}06${GS}9N111234567842${GS}1TABC123${GS}D261231${GS}S1234567890$RS$EOT",
            "]d20104150123456782" + "17261231" + "10ABC123$GS" + "21SN0001"
        )

        val results = inputs.map { PackageCodeParser.parse(it) }

        assertEquals(
            listOf(
                PackageCode("12345678", null, null, null),
                PackageCode("12345678", "ABC123", LocalDate.of(2026, 12, 31), "1234567890"),
                PackageCode("12345678", "ABC123", LocalDate.of(2026, 12, 31), "SN0001")
            ),
            results
        )
    }

    @Test
    fun `should parse gs1 when gtin is not the first field and fnc1 prefix missing or a leading GS`() {
        val inputs = listOf(
            "${GS}17261231" + "10ABC123$GS" + "0104150123456782",
            "17261231" + "0104150123456782" + "10ABC123",
            "21SN0001$GS" + "0104150123456782" + "17261231"
        )

        val pzns = inputs.map { PackageCodeParser.parse(it)?.pzn }

        assertEquals(listOf("12345678", "12345678", "12345678"), pzns)
    }

    @Test
    fun `should return null for unrelated barcodes`() {
        val results = listOf("4006381333931", "https://example.org", "Medicine A").map { PackageCodeParser.parse(it) }

        assertTrue(results.all { it == null }, "results: $results")
    }

    @Test
    fun `should round trip random valid pzns through every format`() {
        val random = Random(42)
        val pzns = generateSequence { pznFor((0 until 7).map { random.nextInt(10) }.joinToString("")) }
            .filterNotNull().take(500).toList()

        val mismatches = pzns.flatMap { pzn ->
            listOf("-$pzn", "9N${ppnFor(pzn)}${GS}D261231", "]d201${gtinFor(pzn)}17261231", "10LOT$GS" + "01${gtinFor(pzn)}").filter { PackageCodeParser.parse(it)?.pzn != pzn }
        }

        assertEquals(emptyList(), mismatches)
    }

    private fun pznFor(base: String): String? {
        val remainder = base.withIndex().sumOf { (i, c) -> (c - '0') * (i + 1) } % 11
        return if (remainder == 10) null else base + remainder
    }

    private fun ppnFor(pzn: String): String {
        val body = "11$pzn"
        val checksum = body.withIndex().sumOf { (i, c) -> c.code * (i + 2) } % 97
        return body + "%02d".format(checksum)
    }

    private fun gtinFor(pzn: String): String {
        val body = "04150$pzn"
        val weighted = body.reversed().withIndex().sumOf { (i, c) -> (c - '0') * if (i % 2 == 0) 3 else 1 }
        return body + (10 - weighted % 10) % 10
    }

    private companion object {
        const val GS = "\u001D"
        const val RS = "\u001E"
        const val EOT = "\u0004"
    }
}
