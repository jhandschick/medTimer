package com.futsch1.medtimer.core.scan

import java.time.DateTimeException
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

private val yymmdd = Regex("(\\d{2})(\\d{2})(\\d{2})")

/** Day `00` means the last day of the month (IFA and GS1 convention). */
internal fun parseExpiry(value: String): LocalDate? {
    val (year, month, day) = yymmdd.matchEntire(value)?.destructured ?: return null
    return try {
        val firstOfMonth = LocalDate.of(2000 + year.toInt(), month.toInt(), 1)
        if (day == "00") firstOfMonth.with(TemporalAdjusters.lastDayOfMonth()) else firstOfMonth.withDayOfMonth(day.toInt())
    } catch (_: DateTimeException) {
        null
    }
}
