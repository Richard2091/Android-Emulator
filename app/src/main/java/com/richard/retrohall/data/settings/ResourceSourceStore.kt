package com.richard.retrohall.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.resourceSourceDataStore by preferencesDataStore(name = "resource_sources")

/**
 * 资源仓库数据源配置：游戏源 / 核心源。留空表示使用内置默认地址。
 */
data class ResourceSources(
    val gameSourceUrl: String = "",
    val coreSourceUrl: String = "",
)

class ResourceSourceStore(private val context: Context) {
    val sources: Flow<ResourceSources> = context.resourceSourceDataStore.data.map { preferences ->
        ResourceSources(
            gameSourceUrl = preferences[Keys.GameSource].orEmpty(),
            coreSourceUrl = preferences[Keys.CoreSource].orEmpty(),
        )
    }

    suspend fun gameSourceUrl(): String =
        context.resourceSourceDataStore.data.map { it[Keys.GameSource].orEmpty() }.first()

    suspend fun coreSourceUrl(): String =
        context.resourceSourceDataStore.data.map { it[Keys.CoreSource].orEmpty() }.first()

    suspend fun update(sources: ResourceSources) {
        context.resourceSourceDataStore.edit { preferences ->
            preferences[Keys.GameSource] = sources.gameSourceUrl.trim()
            preferences[Keys.CoreSource] = sources.coreSourceUrl.trim()
        }
    }

    private object Keys {
        val GameSource = stringPreferencesKey("game_source_url")
        val CoreSource = stringPreferencesKey("core_source_url")
    }
}
