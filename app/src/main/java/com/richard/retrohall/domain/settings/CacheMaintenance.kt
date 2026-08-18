package com.richard.retrohall.domain.settings

interface CacheMaintenance {
    suspend fun totalSize(): Long

    suspend fun clear()

    fun formatBytes(bytes: Long): String
}
