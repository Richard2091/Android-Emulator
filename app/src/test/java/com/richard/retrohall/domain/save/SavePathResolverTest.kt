package com.richard.retrohall.domain.save

import org.junit.Assert.assertEquals
import org.junit.Test

class SavePathResolverTest {
    @Test
    fun resolvesSramPathInsidePrivateFilesRoot() {
        assertEquals(
            "files/saves/sram/mario.srm",
            SavePathResolver.sramPath("files", "mario"),
        )
    }

    @Test
    fun resolvesAutoStatePath() {
        assertEquals(
            "files/saves/states/mario/auto.state",
            SavePathResolver.statePath("files", "mario", SaveSlot.Auto),
        )
    }

    @Test
    fun resolvesManualStatePath() {
        assertEquals(
            "files/saves/states/mario/manual-2.state",
            SavePathResolver.statePath("files", "mario", SaveSlot.Manual(2)),
        )
    }
}
