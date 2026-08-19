package com.richard.retrohall.data.bootstrap

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
        }.getOrDefault(0)
        if (synced == 0) {
            runCatching { gameRepository.syncRemoteCatalog() }
        }
    }
}
