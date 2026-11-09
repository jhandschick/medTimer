package com.futsch1.medtimer.core.scan

/** GS1 element strings: application identifiers (AIs) in sequence, variable-length values terminated by GS (FNC1) or the end. */
internal object Gs1DataMatrixParser {
    private const val GS = '\u001D'
    private const val SYMBOLOGY_IDENTIFIER = "]d2"
    private const val GERMAN_NTIN_PREFIX = "04150"
    private const val AI_GTIN = "01"
    private const val AI_EXPIRY = "17"
    private const val AI_BATCH = "10"
    private const val AI_SERIAL = "21"
    private const val AI_NHRN_PZN = "710"

    // GS1 General Specifications: AI length and the AIs with predefined data length, both keyed by the first two digits.
    private val threeDigitAis = setOf("23", "24", "25", "40", "41", "42", "71")
    private val fourDigitAis = setOf("31", "32", "33", "34", "35", "36", "39", "43", "70", "72", "80", "81", "82")
    private val predefinedDataLength = mapOf("00" to 18, "01" to 14, "02" to 14, "03" to 14, "04" to 16, "20" to 2, "41" to 13) +
            (11..19).associate { "$it" to 6 } + (31..36).associate { "$it" to 6 }

    fun parse(raw: String): PackageCode? {
        val fields = readElements(raw.trim().removePrefix(SYMBOLOGY_IDENTIFIER).trimStart(GS)) ?: return null
        val pzn = pznFrom(fields) ?: return null
        val expiry = fields[AI_EXPIRY]?.let { parseExpiry(it) ?: return null }
        return PackageCode(pzn, fields[AI_BATCH], expiry, fields[AI_SERIAL])
    }

    private fun readElements(data: String): Map<String, String>? {
        val fields = mutableListOf<Pair<String, String>>()
        var position = 0
        while (position < data.length) {
            if (data[position] == GS) {
                position++
                continue
            }
            val prefix = data.substring(position, minOf(position + 2, data.length))
            if (prefix.length < 2 || !prefix.all { it in '0'..'9' }) return null
            val aiLength = if (prefix in fourDigitAis) 4 else if (prefix in threeDigitAis) 3 else 2
            val valueStart = position + aiLength
            if (valueStart > data.length) return null
            val ai = data.substring(position, valueStart)
            val fixedLength = predefinedDataLength[prefix]
            val valueEnd = if (fixedLength != null) valueStart + fixedLength else data.indexOf(GS, valueStart).takeIf { it >= 0 } ?: data.length
            if (valueEnd > data.length) return null
            fields += ai to data.substring(valueStart, valueEnd)
            position = valueEnd
        }
        return fields.toConsistentMap()
    }

    private fun pznFrom(fields: Map<String, String>): String? {
        val gtin = fields[AI_GTIN]
        if (gtin != null && !Checksums.isValidGtin14(gtin)) return null
        val pzn = if (gtin != null && gtin.startsWith(GERMAN_NTIN_PREFIX)) gtin.substring(5, 13) else fields[AI_NHRN_PZN]
        return pzn?.takeIf { Checksums.isValidPzn(it) }
    }
}
