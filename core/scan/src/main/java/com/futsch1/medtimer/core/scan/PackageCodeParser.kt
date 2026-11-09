package com.futsch1.medtimer.core.scan

object PackageCodeParser {
    private val ifaPpnField = Regex("(^|\u001D)9N")

    /** Returns the package data of a PZN Code 39, IFA or GS1 Data Matrix content, or null when it is none of them or invalid. */
    /** True if [pzn] is an eight-digit PZN with a matching check digit. */
    fun isValidPzn(pzn: String): Boolean = Checksums.isValidPzn(pzn)

    fun parse(raw: String): PackageCode? {
        val content = raw.trim()
        if (content.startsWith("[)>") || ifaPpnField.containsMatchIn(content)) return IfaDataMatrixParser.parse(content)
        // Decoders usually drop the leading FNC1, so GS1 has no reliable marker; its GTIN/PZN checksums guard against false hits.
        return Code39PznParser.parse(content) ?: Gs1DataMatrixParser.parse(content)
    }
}
