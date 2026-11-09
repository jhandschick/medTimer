package com.futsch1.medtimer.core.scan

internal object Checksums {
    private val eightDigits = Regex("\\d{8}")
    private val fourteenDigits = Regex("\\d{14}")
    private val ppnPattern = Regex("[0-9A-Z]{3,20}\\d{2}")

    /** PZN8: (Σ digit_i × i, i = 1..7) mod 11 is the eighth digit; remainder 10 is never valid. */
    fun isValidPzn(pzn: String): Boolean {
        if (!eightDigits.matches(pzn)) return false
        val remainder = (0 until 7).sumOf { (pzn[it] - '0') * (it + 1) } % 11
        return remainder != 10 && remainder == pzn[7] - '0'
    }

    /** IFA PPN: Σ ASCII(c_i) × (i + 1) over all but the last two characters, i starting at 1, mod 97, as two digits. */
    fun isValidPpn(ppn: String): Boolean {
        if (!ppnPattern.matches(ppn)) return false
        val body = ppn.dropLast(2)
        val checksum = body.withIndex().sumOf { (index, char) -> char.code * (index + 2) } % 97
        return ppn.takeLast(2).toInt() == checksum
    }

    /** GS1 mod 10: weights 3 and 1 alternate from the rightmost data digit. */
    fun isValidGtin14(gtin: String): Boolean {
        if (!fourteenDigits.matches(gtin)) return false
        val weighted = gtin.dropLast(1).reversed().withIndex().sumOf { (index, char) -> (char - '0') * if (index % 2 == 0) 3 else 1 }
        return (10 - weighted % 10) % 10 == gtin.last() - '0'
    }
}
