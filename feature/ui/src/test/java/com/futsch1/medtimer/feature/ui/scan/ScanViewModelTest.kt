package com.futsch1.medtimer.feature.ui.scan

import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.repository.MedicineRepository
import com.futsch1.medtimer.core.scan.PackageCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class ScanViewModelTest {
    private val handler: ScanHandler = mock()
    private val medicineRepository: MedicineRepository = mock()
    private val medicineA = Medicine.default().copy(id = 1, name = "Medicine A", pzn = "12345678")
    private val code = PackageCode("12345678", "ABC123", LocalDate.of(2026, 12, 31), null)

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = ScanViewModel(handler, medicineRepository)

    @Test
    fun `should stay scanning when code is not a package`() = runTest {
        whenever(handler.lookup("x")).thenReturn(ScanOutcome.NotAPackage)
        val viewModel = viewModel()

        viewModel.onRaw("x")

        assertEquals(ScanUiState.Scanning, viewModel.state.value)
    }

    @Test
    fun `should show refill dialog when medicine is known`() = runTest {
        whenever(handler.lookup("x")).thenReturn(ScanOutcome.Known(medicineA, code))
        val viewModel = viewModel()

        viewModel.onRaw("x")

        assertEquals(ScanUiState.Found(ScanOutcome.Known(medicineA, code)), viewModel.state.value)
    }

    @Test
    fun `should offer medicines when pzn is unknown`() = runTest {
        whenever(handler.lookup("x")).thenReturn(ScanOutcome.Unknown(code))
        whenever(medicineRepository.getAll()).thenReturn(listOf(medicineA))
        val viewModel = viewModel()

        viewModel.onRaw("x")

        assertEquals(ScanUiState.Unassigned(ScanOutcome.Unknown(code), listOf(medicineA)), viewModel.state.value)
    }

    @Test
    fun `should ignore further codes while a dialog is open`() = runTest {
        whenever(handler.lookup(any())).thenReturn(ScanOutcome.Known(medicineA, code))
        val viewModel = viewModel()

        viewModel.onRaw("x")
        viewModel.onRaw("x")

        verify(handler, times(1)).lookup(any())
    }

    @Test
    fun `should pass expiry only when user keeps it ticked`() = runTest {
        whenever(handler.lookup("x")).thenReturn(ScanOutcome.Known(medicineA, code))
        val viewModel = viewModel()
        viewModel.onRaw("x")

        viewModel.confirmRefill(takeExpiry = false)

        verify(handler).refill(1, null)
        assertIs<ScanUiState.Done>(viewModel.state.value)
    }

    @Test
    fun `should pass expiry when user keeps it ticked`() = runTest {
        whenever(handler.lookup("x")).thenReturn(ScanOutcome.Known(medicineA, code))
        val viewModel = viewModel()
        viewModel.onRaw("x")

        viewModel.confirmRefill(takeExpiry = true)

        verify(handler).refill(1, LocalDate.of(2026, 12, 31))
    }

    @Test
    fun `should assign pzn when user picks a medicine`() = runTest {
        whenever(handler.lookup("x")).thenReturn(ScanOutcome.Unknown(code))
        whenever(medicineRepository.getAll()).thenReturn(listOf(medicineA))
        val viewModel = viewModel()
        viewModel.onRaw("x")

        viewModel.assignTo(1)

        verify(handler).assign(1, "12345678")
        assertIs<ScanUiState.Done>(viewModel.state.value)
    }

    @Test
    fun `should resume scanning when dialog is dismissed`() = runTest {
        whenever(handler.lookup(any())).thenReturn(ScanOutcome.Known(medicineA, code))
        val viewModel = viewModel()
        viewModel.onRaw("x")

        viewModel.dismiss()
        viewModel.onRaw("x")

        verify(handler, times(2)).lookup(any())
    }
}
