package com.futsch1.medtimer.core.scan

/** IFA securPharm codes: ASC MH10 data identifiers separated by GS, optionally wrapped in a Macro 06 envelope. */
internal object IfaDataMatrixParser {
    private const val GS = '\u001D'
    private const val MACRO_06_HEADER = "[)>\u001E06\u001D"
    private const val MACRO_TRAILER = "\u001E\u0004"
    private const val PZN_PPN_PREFIX = "11"
    private val dataIdentifier = Regex("(\\d*[A-Z])(.*)", RegexOption.DOT_MATCHES_ALL)

    fun parse(raw: String): PackageCode? {
        val fields = raw.trim().removePrefix(MACRO_06_HEADER).removeSuffix(MACRO_TRAILER).split(GS)
            .mapNotNull { dataIdentifier.matchEntire(it)?.destructured }
            .associate { (identifier, value) -> identifier to value }
        val pzn = fields["9N"]?.let { pznFromPpn(it) } ?: return null
        val expiry = fields["D"]?.let { parseExpiry(it) ?: return null }
        return PackageCode(pzn, fields["1T"], expiry, fields["S"])
    }

    private fun pznFromPpn(ppn: String): String? {
        if (ppn.length != 12 || !ppn.startsWith(PZN_PPN_PREFIX) || !Checksums.isValidPpn(ppn)) return null
        return ppn.substring(2, 10).takeIf { Checksums.isValidPzn(it) }
    }
}
