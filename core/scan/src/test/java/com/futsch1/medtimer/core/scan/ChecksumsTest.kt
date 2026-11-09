package com.futsch1.medtimer.core.scan

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChecksumsTest {
    @Test
    fun `should accept pzn when check digit matches`() {
        val pzns = listOf("12345678", "01234562", "04785363", "00000000")

        val results = pzns.map { Checksums.isValidPzn(it) }

        assertTrue(results.all { it }, "results: $results")
    }

    @Test
    fun `should reject pzn when check digit differs`() {
        assertFalse(Checksums.isValidPzn("01234567"))
    }

    @Test
    fun `should reject pzn when weighted sum leaves remainder ten`() {
        val pzns = (0..9).map { "1000006$it" }

        val results = pzns.map { Checksums.isValidPzn(it) }

        assertTrue(results.none { it }, "results: $results")
    }

    @Test
    fun `should reject pzn when not exactly eight digits`() {
        val pzns = listOf("1234567", "123456789", "1234567X", "")

        val results = pzns.map { Checksums.isValidPzn(it) }

        assertTrue(results.none { it }, "results: $results")
    }

    @Test
    fun `should accept ppn when mod 97 check digits match`() {
        val ppns = listOf("111234567842", "110123456224")

        val results = ppns.map { Checksums.isValidPpn(it) }

        assertTrue(results.all { it }, "results: $results")
    }

    @Test
    fun `should reject ppn when check digits differ`() {
        assertFalse(Checksums.isValidPpn("111234567843"))
    }

    @Test
    fun `should accept gtin when mod 10 check digit matches`() {
        val gtins = listOf("04150123456782", "04006381333931")

        val results = gtins.map { Checksums.isValidGtin14(it) }

        assertTrue(results.all { it }, "results: $results")
    }

    @Test
    fun `should reject gtin when check digit differs`() {
        assertFalse(Checksums.isValidGtin14("04150123456783"))
    }
}
