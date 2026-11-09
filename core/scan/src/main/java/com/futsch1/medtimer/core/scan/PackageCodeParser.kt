package com.futsch1.medtimer.core.scan

object PackageCodeParser {
    private val ifaPpnField = Regex("(^|\u001D)9N")
    private val gs1Start = Regex("(]d2|\u001D|01\\d{14}).*", RegexOption.DOT_MATCHES_ALL)

    /** Returns the package data of a PZN Code 39, IFA or GS1 Data Matrix content, or null when it is none of them or invalid. */
    fun parse(raw: String): PackageCode? {
        val content = raw.trim()
        return when {
            content.startsWith("[)>") || ifaPpnField.containsMatchIn(content) -> IfaDataMatrixParser.parse(content)
            gs1Start.matches(content) -> Gs1DataMatrixParser.parse(content)
            else -> Code39PznParser.parse(content)
        }
    }
}
