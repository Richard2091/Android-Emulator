package com.richard.retrohall.data.bootstrap

import android.util.Log
import com.richard.retrohall.data.game.GameRepository
import com.richard.retrohall.data.game.ResourceCatalogClient

/**
 * 启动引导：只消费数据源资源。
 *
 * 不注入任何私有资源（游戏与核心都来自数据源）。游戏目录走 v2 目录同步，
 * 核心由核心管理弹窗按需从在线核心仓库下载。
 */
class AppBootstrapper(
    private val gameRepository: GameRepository,
    private val resourceCatalogClient: ResourceCatalogClient,
) {
    suspend fun bootstrap() {
        val synced = runCatching {
            gameRepository.syncFromResourceCatalog(resourceCatalogClient)
        }.onFailure { Log.w(TAG, "v2 目录同步失败", it) }.getOrDefault(0)
        if (synced > 0) {
            Log.i(TAG, "v2 目录同步完成：$synced 个游戏")
            return
        }

        // v2 不可用且本地无数据时，回退 v1 兼容源，避免与 v2 数据混用。
        if (gameRepository.isEmpty()) {
            runCatching { gameRepository.syncRemoteCatalog() }
        }
    }

    companion object {
        private const val TAG = "AppBootstrapper"
    }
}
