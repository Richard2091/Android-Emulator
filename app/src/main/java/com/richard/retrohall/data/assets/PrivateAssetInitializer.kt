package com.richard.retrohall.data.assets

import android.content.Context
import com.richard.retrohall.data.db.LocalGameDao
import com.richard.retrohall.data.db.toEntity
import com.richard.retrohall.domain.game.LocalGame
import java.io.File

sealed interface PrivateAssetResult {
    data object MissingManifest : PrivateAssetResult
    data class Initialized(val gameCount: Int) : PrivateAssetResult
}

/**
 * 负责把构建期注入的私有资源复制到 App 私有目录并写入游戏索引。
 *
 * @param context Android Context。
 * @param localGameDao 游戏 DAO。
 */
class PrivateAssetInitializer(
    private val context: Context,
    private val localGameDao: LocalGameDao,
) {
    /**
     * 执行私有资源初始化。
     *
     * @return 初始化结果。
     */
    suspend fun initialize(): PrivateAssetResult {
        val assetManager = context.assets
        val manifestPath = "retrohall_private/manifest.json"

        // 尝试读取 manifest；缺失时让公开构建继续使用假数据。
        val manifestText = runCatching {
            assetManager.open(manifestPath).bufferedReader(Charsets.UTF_8).use { it.readText() }
        }.getOrNull() ?: return PrivateAssetResult.MissingManifest

        val manifest = PrivateAssetManifest.parse(manifestText)
        val filesRoot = context.filesDir

        // 复制每个游戏的 ROM 和封面，并转换为 Room 实体。
        val games = manifest.games.map { game ->
            val romTarget = File(filesRoot, game.rom)
            copyAssetIfPresent("retrohall_private/${game.rom}", romTarget)

            val coverTarget = if (game.cover.isNotBlank()) {
                File(filesRoot, game.cover).also { copyAssetIfPresent("retrohall_private/${game.cover}", it) }
            } else {
                null
            }

            LocalGame(
                id = game.id,
                title = game.title,
                platform = game.platform,
                category = game.category,
                coverPath = coverTarget?.absolutePath.orEmpty(),
                romPath = romTarget.absolutePath,
            ).toEntity()
        }

        // 复制 core 到 files/cores/{abi}/，后续 libretro host 从这里加载。
        manifest.cores.values.forEach { abiMap ->
            abiMap.values.forEach { corePath ->
                copyAssetIfPresent("retrohall_private/$corePath", File(filesRoot, corePath))
            }
        }

        localGameDao.upsertAll(games)
        return PrivateAssetResult.Initialized(games.size)
    }

    private fun copyAssetIfPresent(assetPath: String, target: File) {
        // 公开构建可能只有 manifest 示例而没有真实资源，复制失败不应导致 App 崩溃。
        runCatching {
            target.parentFile?.mkdirs()
            context.assets.open(assetPath).use { input ->
                target.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
    }
}
