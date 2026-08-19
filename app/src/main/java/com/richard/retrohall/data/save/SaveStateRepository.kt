package com.richard.retrohall.data.save

import android.content.Context
import com.richard.retrohall.data.db.SaveStateDao
import com.richard.retrohall.data.db.SaveStateEntity
import com.richard.retrohall.domain.save.SavePathResolver
import com.richard.retrohall.domain.save.SaveSlot
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

    override suspend fun upsert(gameId: String, slot: SaveSlot): SaveStateSlot = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val entity = entityFor(gameId, slot, now)
        saveStateDao.upsert(entity)
        entity.toSlot()
    }

    override suspend fun addSlot(gameId: String): SaveStateSlot = withContext(Dispatchers.IO) {
        val nextIndex = nextManualIndex(gameId) ?: throw IllegalStateException("手动存档槽已满")
        val now = System.currentTimeMillis()
        val slot = SaveSlot.Manual(nextIndex)
        val entity = SaveStateEntity(
            id = saveId(gameId, slot),
            gameId = gameId,
            slotType = "manual",
            slotIndex = nextIndex,
            filePath = stateFile(gameId, slot).absolutePath,
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
        File(SavePathResolver.sramPath(filesRoot.absolutePath, gameId)).takeIf { it.exists() }?.delete()
        saveStateDao.deleteByGameId(gameId)
    }

    override suspend fun copy(slotId: String): SaveStateSlot? = withContext(Dispatchers.IO) {
        val saveState = saveStateDao.getById(slotId) ?: return@withContext null
        val nextIndex = nextManualIndex(saveState.gameId) ?: return@withContext null
        val now = System.currentTimeMillis()
        val target = stateFile(saveState.gameId, SaveSlot.Manual(nextIndex))
        val source = File(saveState.filePath)
        if (!source.isFile) return@withContext null
        target.parentFile?.mkdirs()
        source.copyTo(target, overwrite = true)
        val entity = SaveStateEntity(
            id = saveId(saveState.gameId, SaveSlot.Manual(nextIndex)),
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

    private fun entityFor(gameId: String, slot: SaveSlot, now: Long): SaveStateEntity {
        return when (slot) {
            SaveSlot.Auto -> SaveStateEntity(
                id = saveId(gameId, slot),
                gameId = gameId,
                slotType = "auto",
                slotIndex = null,
                filePath = stateFile(gameId, slot).absolutePath,
                createdAt = now,
                updatedAt = now,
            )
            is SaveSlot.Manual -> SaveStateEntity(
                id = saveId(gameId, slot),
                gameId = gameId,
                slotType = "manual",
                slotIndex = slot.index,
                filePath = stateFile(gameId, slot).absolutePath,
                createdAt = now,
                updatedAt = now,
            )
        }
    }

    private suspend fun nextManualIndex(gameId: String): Int? {
        val used = saveStateDao.getForGame(gameId).mapNotNull { it.slotIndex }.toSet()
        return (1..3).firstOrNull { it !in used }
    }

    private fun stateFile(gameId: String, slot: SaveSlot): File {
        return File(SavePathResolver.statePath(filesRoot.absolutePath, gameId, slot))
    }

    private fun saveId(gameId: String, slot: SaveSlot): String = when (slot) {
        SaveSlot.Auto -> "$gameId-auto"
        is SaveSlot.Manual -> "$gameId-manual-${slot.index}"
    }

    private fun SaveStateEntity.toSlot(): SaveStateSlot {
        return SaveStateSlot(
            id = id,
            slotType = slotType,
            slotIndex = slotIndex,
            updatedAt = updatedAt,
        )
    }
}
