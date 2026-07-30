package com.richard.retrohall.domain.save

sealed interface SaveSlot {
    data object Auto : SaveSlot
    data class Manual(val index: Int) : SaveSlot {
        init {
            require(index in 1..3) { "Manual save slot index must be 1, 2, or 3." }
        }
    }
}
