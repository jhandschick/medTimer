package com.futsch1.medtimer.feature.ui.scan

import com.futsch1.medtimer.core.common.time.TimeAccess
import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.repository.MedicineRepository
import com.futsch1.medtimer.core.scan.PackageCode
import kotlinx.coroutines.CompletableDeferred
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
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ScanViewModelTest {
    private val handler: ScanHandler = mock()
    private val medicineRepository: MedicineRepository = mock()
    private var now = Instant.parse("2026-11-09T12:00:00Z")
    private val timeAccess: TimeAccess = mock { on { now() } doAnswer { now } }
    private val medicineA = Medicine.default().copy(id = 1, name = "Medicine A", pzn = "12345678")
    private val code = PackageCode("12345678", "ABC123", LocalDate.of(2026, 12, 31), null)

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = ScanViewModel(handler, medicineRepository, timeAccess)

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
    fun `should drop expiry when user unticks it`() = runTest {
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
        viewModel.onRaw("y")

        verify(handler, times(2)).lookup(any())
    }

    @Test
    fun `should refill once when confirm is tapped twice`() = runTest {
        val gate = CompletableDeferred<Unit>()
        whenever(handler.lookup("x")).thenReturn(ScanOutcome.Known(medicineA, code))
        whenever(handler.refill(any(), anyOrNull())).doSuspendableAnswer { gate.await() }
        val viewModel = viewModel()
        viewModel.onRaw("x")

        viewModel.confirmRefill(takeExpiry = true)
        viewModel.confirmRefill(takeExpiry = true)
        gate.complete(Unit)

        verify(handler, times(1)).refill(any(), anyOrNull())
        assertIs<ScanUiState.Done>(viewModel.state.value)
    }

    @Test
    fun `should ignore the dismissed code while it stays in view`() = runTest {
        whenever(handler.lookup(any())).thenReturn(ScanOutcome.Known(medicineA, code))
        val viewModel = viewModel()
        viewModel.onRaw("x")
        viewModel.dismiss()

        now = now.plusSeconds(2)
        viewModel.onRaw("x")

        verify(handler, times(1)).lookup(any())
        assertEquals(ScanUiState.Scanning, viewModel.state.value)
    }

    @Test
    fun `should accept the dismissed code again after a pause`() = runTest {
        whenever(handler.lookup(any())).thenReturn(ScanOutcome.Known(medicineA, code))
        val viewModel = viewModel()
        viewModel.onRaw("x")
        viewModel.dismiss()

        now = now.plusSeconds(4)
        viewModel.onRaw("x")

        verify(handler, times(2)).lookup(any())
    }

    @Test
    fun `should report camera unavailable when binding fails`() {
        val viewModel = viewModel()
        assertTrue(viewModel.cameraAvailable.value)

        viewModel.onCameraUnavailable()

        assertFalse(viewModel.cameraAvailable.value)
    }
}
