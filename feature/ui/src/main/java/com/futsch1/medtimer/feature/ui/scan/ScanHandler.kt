package com.futsch1.medtimer.feature.ui.scan

import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.repository.MedicineRepository
import com.futsch1.medtimer.core.scan.PackageCode
import com.futsch1.medtimer.core.scan.PackageCodeParser
import com.futsch1.medtimer.feature.reminders.api.command.ReminderCommandBus
import java.time.LocalDate
import javax.inject.Inject

sealed interface ScanOutcome {
    data object NotAPackage : ScanOutcome
    data class Known(val medicine: Medicine, val code: PackageCode) : ScanOutcome
    data class Unknown(val code: PackageCode) : ScanOutcome
}

/** Turns a scanned barcode into a medicine and applies what the user confirmed: a refill or a PZN assignment. */
class ScanHandler @Inject constructor(
    private val medicineRepository: MedicineRepository,
    private val commandBus: ReminderCommandBus
) {
    suspend fun lookup(raw: String): ScanOutcome {
        val code = PackageCodeParser.parse(raw) ?: return ScanOutcome.NotAPackage
        return medicineRepository.findByPzn(code.pzn)
            ?.let { ScanOutcome.Known(it, code) }
            ?: ScanOutcome.Unknown(code)
    }

    suspend fun refill(medicineId: Int, expiry: LocalDate?) {
        commandBus.processRefill(medicineId)
        if (expiry != null) {
            // Fetch after the refill so the new stock amount is not overwritten by a stale copy
            updateMedicine(medicineId) { it.copy(expirationDate = expiry) }
        }
    }

    suspend fun assign(medicineId: Int, pzn: String) {
        updateMedicine(medicineId) { it.copy(pzn = pzn) }
    }

    private suspend fun updateMedicine(medicineId: Int, change: (Medicine) -> Medicine) {
        medicineRepository.fetch(medicineId)?.let { medicineRepository.update(change(it)) }
    }
}
