package com.futsch1.medtimer.core.scan

import java.time.LocalDate

data class PackageCode(val pzn: String, val batch: String?, val expiry: LocalDate?, val serial: String?)
