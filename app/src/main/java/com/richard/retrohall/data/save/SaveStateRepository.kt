package com.richard.retrohall.data.save

import android.content.Context
import com.richard.retrohall.data.db.SaveStateDao
import com.richard.retrohall.data.db.SaveStateEntity
import com.richard.retrohall.domain.save.SaveStateSlot
import com.richard.retrohall.domain.save.SaveStateStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

class SaveStateRepository(
    context: Context,
    private val saveStateDao: SaveStateDao,
) : SaveStateStore {
    private val filesRoot = context.applicationContext.filesDir

    override fun observeForGame(gameId: String): Flow<List<SaveStateSlot>> {
        return saveStateDao.observeForGame(gameId).map { slots -> slots.map { it.toSlot() } }
    }

    override suspend fun addSlot(gameId: String): SaveStateSlot = withContext(Dispatchers.IO) {
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
        entity.toSlot()
    }

    override suspend fun delete(slotId: String) = withContext(Dispatchers.IO) {
        val saveState = saveStateDao.getById(slotId)
        if (saveState != null) {
            File(saveState.filePath).takeIf { it.exists() }?.delete()
        }
        saveStateDao.deleteById(slotId)
    }

    override suspend fun deleteForGame(gameId: String) = withContext(Dispatchers.IO) {
        File(filesRoot, "saves/states/$gameId").deleteRecursively()
        saveStateDao.deleteByGameId(gameId)
    }

    override suspend fun copy(slotId: String): SaveStateSlot? = withContext(Dispatchers.IO) {
        val saveState = saveStateDao.getById(slotId) ?: return@withContext null
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
        entity.toSlot()
    }

    private suspend fun nextManualIndex(gameId: String): Int {
        val used = saveStateDao.getForGame(gameId).mapNotNull { it.slotIndex }.toSet()
        return generateSequence(1) { it + 1 }.first { it !in used }
    }

    private fun stateFile(gameId: String, index: Int): File {
        return File(filesRoot, "saves/states/$gameId/manual-$index.state")
    }

    private fun saveId(gameId: String, index: Int): String = "$gameId-manual-$index"

    private fun SaveStateEntity.toSlot(): SaveStateSlot {
        return SaveStateSlot(
            id = id,
            slotType = slotType,
            slotIndex = slotIndex,
            updatedAt = updatedAt,
        )
    }
}
