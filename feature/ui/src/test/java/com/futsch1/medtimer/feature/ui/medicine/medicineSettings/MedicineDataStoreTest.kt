package com.futsch1.medtimer.feature.ui.medicine.medicineSettings

import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.core.domain.repository.MedicineRepository
import com.futsch1.medtimer.core.ui.TimeFormatter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verifyBlocking
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class MedicineDataStoreTest {
    private val medicineRepository: MedicineRepository = mock()
    private val medicineA = Medicine.default().copy(name = "Medicine A")

    private fun dataStore(medicine: Medicine = medicineA) =
        MedicineDataStore(medicine, medicineRepository, mock<TimeFormatter>(), TestScope(UnconfinedTestDispatcher()))

    @Test
    fun `should store pzn when valid`() {
        dataStore().putString("pzn", "12345678")

        verifyBlocking(medicineRepository) { update(argThat { pzn == "12345678" && name == "Medicine A" }) }
    }

    @Test
    fun `should clear pzn when input is blank`() {
        val store = dataStore(medicineA.copy(pzn = "12345678"))

        store.putString("pzn", "  ")

        assertNull(store.modelData.pzn)
        verifyBlocking(medicineRepository) { update(argThat { pzn == null }) }
    }

    @Test
    fun `should keep pzn when input is invalid`() {
        val store = dataStore(medicineA.copy(pzn = "12345678"))

        store.putString("pzn", "12345679")

        assertEquals("12345678", store.modelData.pzn)
        verifyBlocking(medicineRepository, never()) { update(any()) }
    }

    @Test
    fun `should return empty string when pzn unset`() {
        assertEquals("", dataStore().getString("pzn", null))
    }
}
