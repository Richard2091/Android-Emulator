package com.richard.retrohall.domain.save

data class SaveStateSlot(
    val id: String,
    val slotType: String,
    val slotIndex: Int?,
    val updatedAt: Long?,
)
