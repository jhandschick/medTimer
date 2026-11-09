package com.futsch1.medtimer.database

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.futsch1.medtimer.core.domain.model.Medicine
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MedicineRepositoryImplTest {
    private lateinit var database: MedicineRoomDatabase
    private lateinit var repository: MedicineRepositoryImpl

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, MedicineRoomDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = MedicineRepositoryImpl(database.medicineDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `should find medicine when pzn matches`() = runBlocking {
        repository.create(Medicine.default().copy(name = "Medicine A", pzn = "01234562"))
        val idB = repository.create(Medicine.default().copy(name = "Medicine B", pzn = "12345678"))

        val found = repository.findByPzn("12345678")

        assertEquals(idB, found?.id)
    }

    @Test
    fun `should return null when no medicine has the pzn`() = runBlocking {
        repository.create(Medicine.default().copy(name = "Medicine A", pzn = "01234562"))

        assertNull(repository.findByPzn("12345678"))
    }
}
