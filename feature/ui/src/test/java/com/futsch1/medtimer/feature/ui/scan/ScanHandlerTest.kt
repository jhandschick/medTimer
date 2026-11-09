package com.futsch1.medtimer.feature.ui.scan

import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.repository.MedicineRepository
import com.futsch1.medtimer.core.scan.PackageCode
import com.futsch1.medtimer.feature.reminders.api.command.ReminderCommandBus
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argThat
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate
import kotlin.test.assertEquals

class ScanHandlerTest {
    private val medicineRepository: MedicineRepository = mock()
    private val commandBus: ReminderCommandBus = mock()
    private val handler = ScanHandler(medicineRepository, commandBus)
    private val medicineA = Medicine.default().copy(id = 1, name = "Medicine A", pzn = "12345678")
    private val ifaRaw = "[)>${RS}06${GS}9N111234567842${GS}D261231$RS$EOT"

    @Test
    fun `should report not a package when code is unrelated`() = runTest {
        assertEquals(ScanOutcome.NotAPackage, handler.lookup("4006381333931"))
        verify(medicineRepository, never()).findByPzn(any())
    }

    @Test
    fun `should find medicine when pzn is stored`() = runTest {
        whenever(medicineRepository.findByPzn("12345678")).thenReturn(medicineA)

        assertEquals(ScanOutcome.Known(medicineA, PackageCode("12345678", null, null, null)), handler.lookup("PZN-12345678"))
    }

    @Test
    fun `should report unknown when no medicine has the pzn`() = runTest {
        whenever(medicineRepository.findByPzn(any())).thenReturn(null)

        assertEquals(ScanOutcome.Unknown(PackageCode("12345678", null, LocalDate.of(2026, 12, 31), null)), handler.lookup(ifaRaw))
    }

    @Test
    fun `should refill and set expiry when expiry is given`() = runTest {
        whenever(medicineRepository.fetch(1)).thenReturn(medicineA)

        handler.refill(1, LocalDate.of(2026, 12, 31))

        verify(commandBus).processRefill(1)
        verify(medicineRepository).update(argThat { expirationDate == LocalDate.of(2026, 12, 31) && name == "Medicine A" })
    }

    @Test
    fun `should update expiry on the refilled medicine state`() = runTest {
        whenever(medicineRepository.fetch(1)).thenReturn(medicineA)

        handler.refill(1, LocalDate.of(2026, 12, 31))

        inOrder(commandBus, medicineRepository) {
            verify(commandBus).processRefill(1)
            verify(medicineRepository).fetch(1)
            verify(medicineRepository).update(any())
        }
    }

    @Test
    fun `should refill without touching medicine when expiry is null`() = runTest {
        handler.refill(1, null)

        verify(commandBus).processRefill(1)
        verify(medicineRepository, never()).update(anyOrNull())
    }

    @Test
    fun `should store pzn when assigning`() = runTest {
        whenever(medicineRepository.fetch(1)).thenReturn(medicineA.copy(pzn = null))

        handler.assign(1, "12345678")

        verify(medicineRepository).update(argThat { pzn == "12345678" && name == "Medicine A" })
    }

    private companion object {
        const val GS = "\u001D"
        const val RS = "\u001E"
        const val EOT = "\u0004"
    }
}
