package com.futsch1.medtimer.database.toBackup

import com.futsch1.medtimer.database.MedicineEntity
import org.junit.Test
import kotlin.test.assertEquals

class MedicineBackupMapperTest {
    @Test
    fun `should keep pzn when medicine goes through backup and restore`() {
        val entity = MedicineEntity(name = "Medicine A", pzn = "12345678")

        val restored = entity.toBackup().toEntity()

        assertEquals("12345678", restored.pzn)
    }
}
