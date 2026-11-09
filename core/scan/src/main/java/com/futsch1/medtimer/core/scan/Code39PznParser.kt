package com.futsch1.medtimer.core.scan

internal object Code39PznParser {
    private val pattern = Regex("(?:PZN)?-(\\d{8})")

    fun parse(raw: String): PackageCode? {
        val pzn = pattern.matchEntire(raw.trim())?.groupValues?.get(1) ?: return null
        return if (Checksums.isValidPzn(pzn)) PackageCode(pzn, null, null, null) else null
    }
}
