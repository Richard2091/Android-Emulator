package com.richard.retrohall.data.game

/**
 * 资源仓库配置：游戏目录、核心仓库入口及旧版兼容回退。
 *
 * 默认指向维护者发布的 GitHub Pages；旧 FC_ROMS 地址仅作兼容回退。
 */
object ResourceRepositoryConfig {
    /** 游戏目录仓库（v2）根地址，用于读取 catalog/index.v2.json。 */
    const val GAME_CATALOG_BASE_URL = "https://richard2091.github.io/RetroGame/"

    /** 核心仓库根地址，用于读取 catalog/core-manifest.v1.json。 */
    const val CORE_CATALOG_BASE_URL = "https://richard2091.github.io/RetroGame-Cores/"

    /** v1 兼容清单：新仓库 legacy 投影优先，旧 FC_ROMS 兜底。 */
    val LEGACY_MANIFEST_URLS = listOf(
        "https://richard2091.github.io/RetroGame/legacy/manifest.v1.json",
        "https://raw.githubusercontent.com/Richard2091/RetroGame/main/legacy/manifest.v1.json",
        "https://richard2091.github.io/FC_ROMS/manifest.v1.json",
        "https://raw.githubusercontent.com/Richard2091/FC_ROMS/main/manifest.v1.json",
    )
}
