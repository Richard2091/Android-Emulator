package com.richard.retrohall.data.save

import android.content.Context
import com.richard.retrohall.data.db.SaveStateDao
import com.richard.retrohall.data.db.SaveStateEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class SaveStateRepository(
    context: Context,
    private val saveStateDao: SaveStateDao,
) {
    private val filesRoot = context.applicationContext.filesDir

    fun observeForGame(gameId: String): Flow<List<SaveStateEntity>> = saveStateDao.observeForGame(gameId)

    suspend fun addSlot(gameId: String): SaveStateEntity = withContext(Dispatchers.IO) {
        val nextIndex = nextManualIndex(gameId)
        val now = System.currentTimeMillis()
        val entity = SaveStateEntity(
            id = saveId(gameId, nextIndex),
            gameId = gameId,
            slotType = "manual",
            slotIndex = nextIndex,
            filePath = stateFile(gameId, nextIndex).absolutePath,
            createdAt = now,
            updatedAt = now,
        )
        saveStateDao.upsert(entity)
        entity
    }

    suspend fun delete(saveState: SaveStateEntity) = withContext(Dispatchers.IO) {
        File(saveState.filePath).takeIf { it.exists() }?.delete()
        saveStateDao.deleteById(saveState.id)
    }

    suspend fun deleteForGame(gameId: String) = withContext(Dispatchers.IO) {
        File(filesRoot, "saves/states/$gameId").deleteRecursively()
        saveStateDao.deleteByGameId(gameId)
    }

    suspend fun copy(saveState: SaveStateEntity): SaveStateEntity = withContext(Dispatchers.IO) {
        val nextIndex = nextManualIndex(saveState.gameId)
        val now = System.currentTimeMillis()
        val target = stateFile(saveState.gameId, nextIndex)
        val source = File(saveState.filePath)
        if (source.isFile && source.length() > 0L) {
            target.parentFile?.mkdirs()
            source.copyTo(target, overwrite = true)
        }
        val entity = SaveStateEntity(
            id = saveId(saveState.gameId, nextIndex),
            gameId = saveState.gameId,
            slotType = "manual",
            slotIndex = nextIndex,
            filePath = target.absolutePath,
            createdAt = now,
            updatedAt = now,
        )
        saveStateDao.upsert(entity)
        entity
    }

    private suspend fun nextManualIndex(gameId: String): Int {
        val used = saveStateDao.getForGame(gameId).mapNotNull { it.slotIndex }.toSet()
        return generateSequence(1) { it + 1 }.first { it !in used }
    }

    private fun stateFile(gameId: String, index: Int): File {
        return File(filesRoot, "saves/states/$gameId/manual-$index.state")
    }

    private fun saveId(gameId: String, index: Int): String = "$gameId-manual-$index"
}
