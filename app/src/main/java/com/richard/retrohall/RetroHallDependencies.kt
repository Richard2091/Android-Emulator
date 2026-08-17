package com.richard.retrohall

import android.content.Context
import androidx.room.Room
import com.richard.retrohall.data.db.RetroHallDatabase
import com.richard.retrohall.data.assets.PrivateAssetInitializer
import com.richard.retrohall.data.game.GitHubGameCatalogClient
import com.richard.retrohall.data.game.GameRepository
import com.richard.retrohall.data.game.RomDownloadManager
import com.richard.retrohall.data.settings.UserSettingsStore
import com.richard.retrohall.emulator.EmulatorSessionFactory

/**
 * 提供应用级依赖，避免在第一版引入额外依赖注入框架。
 */
class RetroHallDependencies private constructor(context: Context) {
    private val appContext = context.applicationContext

    val database: RetroHallDatabase = Room.databaseBuilder(
        appContext,
        RetroHallDatabase::class.java,
        "retrohall.db",
    ).build()

    val gameRepository: GameRepository = GameRepository(database.localGameDao(), GitHubGameCatalogClient())
    val romDownloadManager: RomDownloadManager = RomDownloadManager(appContext)
    val emulatorSessionFactory: EmulatorSessionFactory = EmulatorSessionFactory(appContext)
    val userSettingsStore: UserSettingsStore = UserSettingsStore(appContext)
    val privateAssetInitializer: PrivateAssetInitializer = PrivateAssetInitializer(appContext, database.localGameDao())

    companion object {
        @Volatile
        private var instance: RetroHallDependencies? = null

        /**
         * 获取应用级依赖单例。
         *
         * @param context Android Context。
         * @return 依赖容器实例。
         */
        fun get(context: Context): RetroHallDependencies {
            // 优先返回已有实例，避免重复创建数据库连接。
            return instance ?: synchronized(this) {
                // 双重检查，确保多线程初始化时只创建一次。
                instance ?: RetroHallDependencies(context).also { instance = it }
            }
        }
    }
}
