package com.richard.retrohall.data.bootstrap

import com.richard.retrohall.data.assets.PrivateAssetInitializer
import com.richard.retrohall.data.game.GameRepository

class AppBootstrapper(
    private val privateAssetInitializer: PrivateAssetInitializer?,
    private val gameRepository: GameRepository,
) {
    suspend fun bootstrap() {
        privateAssetInitializer?.initialize()
        gameRepository.seedIfEmpty()
        runCatching { gameRepository.syncRemoteCatalog() }
    }
}
