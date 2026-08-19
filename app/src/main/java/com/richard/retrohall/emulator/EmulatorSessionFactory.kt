package com.richard.retrohall.emulator

import android.content.Context
import android.os.Build
import com.richard.retrohall.data.core.CoreCatalogClient
import com.richard.retrohall.data.core.CoreSelectionStore
import com.richard.retrohall.domain.game.LocalGame

data class EmulatorLaunchResult(
    val session: EmulatorSession,
    val message: String?,
)

class EmulatorSessionFactory(
    context: Context,
    private val coreCatalogClient: CoreCatalogClient,
    private val coreSelectionStore: CoreSelectionStore,
) {
    private val filesRoot = context.applicationContext.filesDir
    private val corePathResolver = CorePathResolver(
        filesRoot,
        Build.SUPPORTED_ABIS.toList(),
        coreSelectionStore,
    )

    suspend fun createStartedSession(game: LocalGame): EmulatorLaunchResult {
        val catalog = runCatching { coreCatalogClient.fetchCatalog() }.getOrNull()
        val core = corePathResolver.resolve(game.platform, catalog)
            ?: return fakeSession(game, "没有检测到 ${game.platform} 的模拟器核心，已使用兼容演示模式。")

        val realSession = runCatching {
            LibretroEmulatorSession(core.file.absolutePath, filesRoot.absolutePath).also { session ->
                session.load(game)
                session.start()
            }
        }.getOrElse {
            return fakeSession(game, "模拟器核心启动失败，已使用兼容演示模式。")
        }

        if (realSession.state != EmulatorState.Running) {
            realSession.stop()
            return fakeSession(game, "模拟器核心未能加载 ROM，已使用兼容演示模式。")
        }

        return EmulatorLaunchResult(session = realSession, message = null)
    }

    private fun fakeSession(game: LocalGame, message: String): EmulatorLaunchResult {
        val session = FakeEmulatorSession().also {
            it.load(game)
            it.start()
        }
        return EmulatorLaunchResult(session = session, message = message)
    }
}
