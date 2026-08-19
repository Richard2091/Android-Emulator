package com.richard.retrohall.data.save

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.room.Room
import com.richard.retrohall.data.db.RetroHallDatabase
import com.richard.retrohall.domain.save.SaveSlot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class SaveStateRepositoryTest {
    private lateinit var context: Context
    private lateinit var db: RetroHallDatabase
    private lateinit var repository: SaveStateRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, RetroHallDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = SaveStateRepository(context, db.saveStateDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun upsertAutoStateUsesAutoPath() = runBlocking {
        val slot = repository.upsert("mario", SaveSlot.Auto)
        assertEquals("mario-auto", slot.id)
        assertEquals("auto", slot.slotType)
        assertEquals(
            File(context.filesDir, "saves/states/mario/auto.state").absolutePath,
            db.saveStateDao().getById(slot.id)?.filePath,
        )
    }

    @Test
    fun addSlotAndCopyRespectManualLimit() = runBlocking {
        val first = repository.addSlot("mario")
        val second = repository.addSlot("mario")
        val third = repository.addSlot("mario")
        assertEquals(1, first.slotIndex)
        assertEquals(2, second.slotIndex)
        assertEquals(3, third.slotIndex)
        try {
            repository.addSlot("mario")
            throw AssertionError("should fail when slots are full")
        } catch (_: IllegalStateException) {
        }
    }

    @Test
    fun deleteForGameRemovesStateAndSramFiles() = runBlocking {
        val sram = File(context.filesDir, "saves/sram/mario.srm")
        sram.parentFile?.mkdirs()
        sram.writeText("sram")
        val stateDir = File(context.filesDir, "saves/states/mario")
        stateDir.mkdirs()
        File(stateDir, "manual-1.state").writeText("state")
        repository.upsert("mario", SaveSlot.Manual(1))

        repository.deleteForGame("mario")

        assertFalse(sram.exists())
        assertFalse(stateDir.exists())
        assertTrue(db.saveStateDao().getForGame("mario").isEmpty())
    }
}
