package com.richard.retrohall.domain.save

object SavePathResolver {
    fun sramPath(filesRoot: String, gameId: String): String =
        "$filesRoot/saves/sram/$gameId.srm"

    fun statePath(filesRoot: String, gameId: String, slot: SaveSlot): String = when (slot) {
        SaveSlot.Auto -> "$filesRoot/saves/states/$gameId/auto.state"
        is SaveSlot.Manual -> "$filesRoot/saves/states/$gameId/manual-${slot.index}.state"
    }
}
