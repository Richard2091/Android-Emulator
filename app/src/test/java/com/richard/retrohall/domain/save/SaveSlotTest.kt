package com.richard.retrohall.domain.save

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SaveSlotTest {
    @Test
    fun manualSlotAcceptsIndexesOneToThree() {
        assertEquals(1, SaveSlot.Manual(1).index)
        assertEquals(2, SaveSlot.Manual(2).index)
        assertEquals(3, SaveSlot.Manual(3).index)
    }

    @Test
    fun manualSlotRejectsIndexesOutsideOneToThree() {
        assertThrows(IllegalArgumentException::class.java) {
            SaveSlot.Manual(0)
        }

        assertThrows(IllegalArgumentException::class.java) {
            SaveSlot.Manual(4)
        }
    }
}
