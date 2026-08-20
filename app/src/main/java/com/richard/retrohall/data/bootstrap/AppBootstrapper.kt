package com.richard.retrohall.data.bootstrap

import android.util.Log
import com.richard.retrohall.data.assets.PrivateAssetInitializer
import com.richard.retrohall.data.game.GameRepository
import com.richard.retrohall.data.game.ResourceCatalogClient

class AppBootstrapper(
    private val privateAssetInitializer: PrivateAssetInitializer?,
    private val gameRepository: GameRepository,
    private val resourceCatalogClient: ResourceCatalogClient,
) {
    suspend fun bootstrap() {
        privateAssetInitializer?.initialize()
        gameRepository.seedIfEmpty()

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
