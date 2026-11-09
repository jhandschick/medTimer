package com.futsch1.medtimer.database.toModel

import com.futsch1.medtimer.core.domain.model.Medicine
import com.futsch1.medtimer.database.FullMedicineEntity
import org.junit.Test
import kotlin.test.assertEquals

class MedicineMapperTest {
    @Test
    fun `should keep pzn when mapping medicine to entity and back`() {
        val medicine = Medicine.default().copy(name = "Medicine A", pzn = "12345678")

        val roundTripped = FullMedicineEntity(medicine.toEntity()).toModel()

        assertEquals("12345678", roundTripped.pzn)
    }
}
