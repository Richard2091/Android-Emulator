package com.richard.retrohall.data.assets

import org.json.JSONObject

data class PrivateAssetManifest(
    val games: List<PrivateAssetGame>,
    val cores: Map<String, Map<String, String>>,
) {
    companion object {
        /**
         * 从 JSON 文本解析私有资源 manifest。
         *
         * @param text manifest JSON 文本。
         * @return 解析后的 manifest。
         */
        fun parse(text: String): PrivateAssetManifest {
            // 解析顶层对象和游戏数组。
            val root = JSONObject(text)
            val gameArray = root.optJSONArray("games")
            val games = buildList {
                if (gameArray != null) {
                    for (index in 0 until gameArray.length()) {
                        val item = gameArray.getJSONObject(index)
                        add(
                            PrivateAssetGame(
                                id = item.getString("id"),
                                title = item.getString("title"),
                                platform = item.getString("platform"),
                                category = item.getString("category"),
                                rom = item.getString("rom"),
                                cover = item.optString("cover", ""),
                            )
                        )
                    }
                }
            }

            // 解析 core 映射，保留平台和 ABI 两级结构。
            val coresObject = root.optJSONObject("cores")
            val cores = mutableMapOf<String, Map<String, String>>()
            if (coresObject != null) {
                coresObject.keys().forEach { platform ->
                    val abiObject = coresObject.getJSONObject(platform)
                    val abiMap = mutableMapOf<String, String>()
                    abiObject.keys().forEach { abi ->
                        abiMap[abi] = abiObject.getString(abi)
                    }
                    cores[platform] = abiMap
                }
            }

            return PrivateAssetManifest(games = games, cores = cores)
        }
    }
}

data class PrivateAssetGame(
    val id: String,
    val title: String,
    val platform: String,
    val category: String,
    val rom: String,
    val cover: String,
)
