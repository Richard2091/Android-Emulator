package com.richard.retrohall

import android.content.Context
import androidx.room.Room
import com.richard.retrohall.data.cache.CacheManager
import com.richard.retrohall.data.bootstrap.AppBootstrapper
import com.richard.retrohall.data.db.RetroHallDatabase
import com.richard.retrohall.data.db.MIGRATION_4_5
import com.richard.retrohall.data.assets.PrivateAssetInitializer
import com.richard.retrohall.data.game.GitHubGameCatalogClient
import com.richard.retrohall.data.game.ContentDownloadManager
import com.richard.retrohall.data.game.CoverDownloader
import com.richard.retrohall.data.game.GameRepository
import com.richard.retrohall.data.game.HasheousGameMetadataClient
import com.richard.retrohall.data.game.ResourceCatalogClient
import com.richard.retrohall.data.game.ResourceRepositoryConfig
import com.richard.retrohall.data.game.RomDownloadManager
import com.richard.retrohall.data.game.ZhMetadataClient
import com.richard.retrohall.data.core.CoreCatalogClient
import com.richard.retrohall.data.core.CoreDownloadManager
import com.richard.retrohall.data.core.CoreSelectionStore
import com.richard.retrohall.data.save.SaveStateRepository
import com.richard.retrohall.data.settings.UserSettingsStore
import com.richard.retrohall.data.settings.ResourceSourceStore
import com.richard.retrohall.domain.game.CoverImageLoader
import com.richard.retrohall.domain.save.SaveStateStore
import com.richard.retrohall.domain.settings.CacheMaintenance
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
    )
        .addMigrations(MIGRATION_4_5)
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()

    val gameRepository: GameRepository = GameRepository(
        database.localGameDao(),
        GitHubGameCatalogClient(),
        HasheousGameMetadataClient(appContext),
        ZhMetadataClient(),
    )
    val resourceSourceStore: ResourceSourceStore = ResourceSourceStore(appContext)
    val resourceCatalogClient: ResourceCatalogClient =
        ResourceCatalogClient(resourceSourceStore)
    val romDownloadManager: RomDownloadManager = RomDownloadManager(appContext)
    val contentDownloadManager: ContentDownloadManager = ContentDownloadManager(appContext)
    val coverDownloader: CoverDownloader = CoverDownloader(appContext)
    val saveStateRepository: SaveStateRepository = SaveStateRepository(appContext, database.saveStateDao())
    val coreCatalogClient: CoreCatalogClient = CoreCatalogClient(resourceSourceStore)
    val coreSelectionStore: CoreSelectionStore = CoreSelectionStore(appContext)
    val coreDownloadManager: CoreDownloadManager = CoreDownloadManager(appContext)
    val emulatorSessionFactory: EmulatorSessionFactory = EmulatorSessionFactory(appContext, coreCatalogClient, coreSelectionStore)
    val userSettingsStore: UserSettingsStore = UserSettingsStore(appContext)
    val privateAssetInitializer: PrivateAssetInitializer = PrivateAssetInitializer(appContext, database.localGameDao())
    val cacheManager: CacheManager = CacheManager(appContext)
    val appBootstrapper: AppBootstrapper = AppBootstrapper(privateAssetInitializer, gameRepository, resourceCatalogClient)
    val coverImageLoader: CoverImageLoader = coverDownloader
    val saveStateStore: SaveStateStore = saveStateRepository
    val cacheMaintenance: CacheMaintenance = cacheManager

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
